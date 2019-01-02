package storage;

import com.google.protobuf.ByteString;
import commons.*;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.value.AtomicValue;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.FileUtils;
import protos.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class GRPCServer {
    private static DistributedList<CrushMap> distributed_crush_maps;
    private static AtomicValue<MetadataTree> distributed_metadata_tree;
    private static DistributedLock metaLock;
    private static String local_id;
    private static int latestCrushMapEpoch;

    // TODO eventually read these settings from config
    public static final int CHUNK_SIZE = 1024 * 1024 * 2; // 2 MB
    public static final String DATAFOLDER = "data/";

    //if we don't have a local list atomix freaks out due to possible concurrency issues
    static List<CrushMap> crushMaps = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(GRPCServer.class.getName());

    private Server server;

    private void start(int port) throws IOException {
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new GetHeartbeatGrpcImpl())
                .addService(new ChunkTransferGrpcImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                GRPCServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }


    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        local_id = args[0];
        String local_ip = args[1];
        int port = Integer.parseInt(args[2]);
        System.out.println("Starting gRPC: " + local_id +  " @ Port: " + port + ".");
        final GRPCServer server = new GRPCServer();

//        System.err.close();

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer(local_id,
                local_ip, port + 100).join();

        distributed_crush_maps = atomix.getList("maps");
        distributed_metadata_tree = atomix.getAtomicValue("mtree");
        metaLock = atomix.getLock("metaLock");
        // TODO improve logic to get from rafts
        crushMaps = new ArrayList<>(distributed_crush_maps);
        distributed_crush_maps.addListener(event -> {
            switch (event.type()) {
                case ADD:
                    System.out.println("Entry added: (" + event.element() + ")" +
                            " | @ epoch: " + event.element().map_epoch);
                    latestCrushMapEpoch = event.element().map_epoch;
                    crushMaps.add(event.element());
                    if(latestCrushMapEpoch > 0)
                        repeerPGs(event.element(), distributed_metadata_tree.get());
                    break;
                case REMOVE:
                    System.out.println("Entry removed: (" + event.element() +")" +
                            " | @ epoch: " + event.element().map_epoch);
                    break;
            }
        });


        // cleanup crew
        ScheduledExecutorService executorService  = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(
                new CleanupRunnable(DATAFOLDER, distributed_metadata_tree),
                0, 30,
                TimeUnit.SECONDS
        );



        server.start(port);
        server.blockUntilShutdown();
    }

    static void repeerPGs(CrushMap crushMap, MetadataTree metadataTree) {
        List<CrushNode> currentOSDNodes = crushMap.get_root().get_children_of_type("osd");

        for (int i = 0; i<Loader.getTotalPgs(); i++) {
            // stop peering if theres a new crushMap version available
            if (latestCrushMapEpoch > crushMap.map_epoch)
                return;
            List<CrushNode> currentPGOSDs = Crush.select_OSDs(crushMap.get_root(), "" + i);
            List<CrushNode> updateOSDs = new ArrayList<>();
            boolean updatePrimary = false;
            boolean updateFailed = false;
            if (currentPGOSDs.size() > 0
                    && currentPGOSDs.get(0).nodeID == Integer.parseInt(local_id)) {
                MetadataPlacementGroup metaPG = metadataTree.getPgs().get(i);
                // TODO improve in decentralization, this update logic only functions because PG logs are centralized!
                List<CrushNode> lastPGOSDs = Crush.select_OSDs(
                        crushMaps.get(metaPG.getLastCompleteRepeerEpoch()).get_root(),
                        "" + i);
                List<CrushNode> aliveLastPGOSDs = currentOSDNodes.stream().filter(
                        n -> !n.isFailed() && lastPGOSDs.stream().anyMatch(o -> o.nodeID == n.nodeID)
                ).collect(Collectors.toList());
                System.out.println("PG: " + i);
//                for (CrushNode node : lastPGOSDs) {
//                    System.out.println("Last PG: " + node.nodeID);
//                }
//                for (CrushNode node : aliveLastPGOSDs) {
//                    System.out.println("Alive PG: " + node.nodeID);
//                }
                if (aliveLastPGOSDs.size() == 0) {
                    // TODO error was here, size was returning 0
                    System.out.println("Entire PG " + i
                            + " OSDs of last updated epoch "
                            + metaPG.getLastCompleteRepeerEpoch()
                            + " are down, PG data temporarily unatainable!");
                    continue;
                }
                for(CrushNode currentNode : currentPGOSDs) {
                    if(lastPGOSDs.stream().noneMatch(o -> o.nodeID == currentNode.nodeID)) {
                        if(currentNode.nodeID == currentPGOSDs.get(0).nodeID) {
                            // must update primary
                            updatePrimary = true;
                        } else {
                            // must update replica
                            updateOSDs.add(currentNode);
                        }
                    }
                }

                if (updatePrimary)
                    System.out.println("Must update new primary: " + lastPGOSDs.get(0).nodeID
                            + " -> " + currentPGOSDs.get(0).nodeID);
                if (updateOSDs.size() > 0) {
                    System.out.print("Must update replicas:");
                    for (CrushNode node : updateOSDs) {
                        System.out.print(" " + node.nodeID);
                    }
                    System.out.println();
                }
                if (updatePrimary || updateOSDs.size() > 0)
                    for (String object : metaPG.getObjects()) {
                        String filename = object.substring(0, object.lastIndexOf("_"));
                        MetadataNode metadataNode = metadataTree.goToNode(filename);
                        if (metadataNode == null || metadataNode.isDeleted()) {
                            // this object is going to be deleted, skip
                            System.out.println("File " + filename + " no longer exists, skipping object "
                                    + object);
                            continue;
                        }
                        String oidWithVersion = object + "_" + metadataNode.getVersion();
                        byte[] data = FileChunkUtils.getObjectFromAnyNodeInList(oidWithVersion, aliveLastPGOSDs);
                        if (data == null) {
                            System.out.println("Couldn't get object for update: " + oidWithVersion);
                            updateFailed = true;
                            break;
                        }
                        System.out.println("Updating object: " + object);
                        if (updatePrimary) {
                            try {
                                System.out.println("Getting object " + object);
                                FileUtils.writeByteArrayToFile(
                                        new File(DATAFOLDER + oidWithVersion),
                                        data);
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("Couldn't store object: " + object);
                                updateFailed = true;
                                break;
                            }
                        } else if (updateOSDs.size() > 0) {
                            for (CrushNode crushNode : updateOSDs) {
                                System.out.println("Sending object to node: " + crushNode.nodeID);
                                if(!FileChunkUtils.postByteArrayToNode(oidWithVersion, data, crushNode)) {
                                    System.out.println("OSD " + crushNode.nodeID + " failed post update: " + oidWithVersion);
                                    updateFailed = true;
                                    break;
                                }
                            }
                        }
                    }
                if (updateFailed) {
                    // skip to next pg
                    continue;
                }
                // if reached this point every OSD in PG was updated successfully
                // TODO trylock and update
                try {
                    if (metaLock.tryLock(10, TimeUnit.SECONDS)) {
                        metadataTree = distributed_metadata_tree.get();
                        metaPG = metadataTree.getPgs().get(i);
                        if (metaPG.getLastCompleteRepeerEpoch() < crushMap.map_epoch)
                            metaPG.setLastCompleteRepeerEpoch(crushMap.map_epoch);
                        distributed_metadata_tree.set(metadataTree);
                        metaLock.unlock();
                        }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Finished repeering!");
    }

    static class GetHeartbeatGrpcImpl extends GetHeartbeatGrpc.GetHeartbeatImplBase {

        @Override
        public void getHeartbeat(HeartbeatRequest req, StreamObserver<HeartbeatReply> responseObserver) {
//            System.out.println("Received heartbeat request.");
            responseObserver.onNext(HeartbeatReply
                    .newBuilder()
                    .setStatus(true)
                    .setOverloaded(false)
                    .build()
            );
            responseObserver.onCompleted();
        }
    }

    static class ChunkTransferGrpcImpl extends ChunkTransferGrpc.ChunkTransferImplBase {

        @Override
        public void getChunk(ChunkOid request, StreamObserver<ChunkData> responseObserver) {
            // TODO test gRPC chunks
            byte[] data;
            System.out.println("Serving chunk: " + request.getOid());
            try {
                data = FileUtils.readFileToByteArray(new File(DATAFOLDER + request.getOid()));
                responseObserver.onNext(ChunkData
                        .newBuilder()
                        .setOid(request.getOid())
                        .setData(ByteString.copyFrom(data))
                        .build());
            } catch (IOException e) {
                e.printStackTrace();
                responseObserver.onNext(null);
            }
            responseObserver.onCompleted();
        }

        @Override
        public void postChunk(ChunkData request, StreamObserver<ChunkPostReply> responseObserver) {
            String oid = request.getOid();
            byte[] data = request.getData().toByteArray();
            System.out.println("Storing chunk: " + request.getOid());
            CrushMap crushMap = distributed_crush_maps.get(distributed_crush_maps.size() - 1);
            String oidWitouthVersion = oid.substring(0, oid.lastIndexOf("_"));
            int pg = Crush.get_pg_id(oidWitouthVersion, Loader.getTotalPgs());
            // TODO fix, currently need to disable this check, because otherwise osds can't peer
            if(!distributed_metadata_tree.get().getPgs().get(pg).active(crushMap.map_epoch) && request.getReplication()) {
                System.out.println("PG " + pg + " is currently inactive and not accepting writes!");
                responseObserver.onNext(ChunkPostReply.newBuilder()
                        .setState(false)
                        .build());
                responseObserver.onCompleted();
                return;
            }
            System.out.println("OSD calculated PG: " + pg);
            ObjectStorageNode node = FileChunkUtils.get_object_primary(oidWitouthVersion, crushMap);

            // TODO improve this logic to use OSD PG's
            if (node != null && node.id == Integer.parseInt(local_id) && request.getReplication()) { // && request.replication
                System.out.println(local_id + " is primary of PG " + pg);
                for (int i = 0; i < Crush.numberOfReplicas; i++) {
                    boolean success = FileChunkUtils.post_object(oid, data, crushMap, i + 1);
                    if (!success) {
                        responseObserver.onNext(ChunkPostReply.newBuilder()
                                .setState(false)
                                .build());
                        responseObserver.onCompleted();
                        return;
                    }
                }
            }

            try {
                FileUtils.writeByteArrayToFile(new File(DATAFOLDER + oid), data);
                // TODO evaluate tradeoff between adding here vs fileChunkUtils (appserver)
                if (node != null && node.id == Integer.parseInt(local_id)) {
                    if (metaLock.tryLock(10, TimeUnit.SECONDS)) {
                        MetadataTree metadataTree = distributed_metadata_tree.get();
                        // if the oid is already registered no need to redo it
                        if(metadataTree.addObjectToPg(pg, oidWitouthVersion)) {
                            distributed_metadata_tree.set(metadataTree);
                        }
                        metaLock.unlock();
                        responseObserver.onNext(ChunkPostReply.newBuilder()
                                .setState(true)
                                .build());
                    } else {
                        System.out.println("Failed to acquire metadata lock!");
                        responseObserver.onNext(ChunkPostReply.newBuilder()
                                .setState(false)
                                .build());
                    }
                } else {
                    responseObserver.onNext(ChunkPostReply.newBuilder()
                            .setState(true)
                            .build());
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("File writing exception!");
                responseObserver.onNext(ChunkPostReply.newBuilder()
                        .setState(false)
                        .build());
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Interrupted before Metatree lock acquired.");
                responseObserver.onNext(ChunkPostReply.newBuilder()
                        .setState(false)
                        .build());
            }
            responseObserver.onCompleted();
        }

    }
}