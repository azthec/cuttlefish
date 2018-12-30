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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;



public class GRPCServer {
    private static DistributedList<CrushMap> distributed_crush_maps;
    private static AtomicValue<MetadataTree> distributed_metadata_tree;
    private static DistributedLock metaLock;
    private static String local_id;

    // TODO eventually read these settings from config
    public static final int CHUNK_SIZE = 1024 * 1024 * 2; // 2 MB
    public static final String DATAFOLDER = "data/";

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
        server.start(port);
        server.blockUntilShutdown();
    }

    static class GetHeartbeatGrpcImpl extends GetHeartbeatGrpc.GetHeartbeatImplBase {

        @Override
        public void getHeartbeat(HeartbeatRequest req, StreamObserver<HeartbeatReply> responseObserver) {
            System.out.println("Received heartbeat request.");
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
            CrushMap crushMap = distributed_crush_maps.get(0);
            String oidWitouthVersion = oid.substring(0, oid.lastIndexOf("_"));
            int pg = Crush.get_pg_id(oidWitouthVersion, Loader.getTotalPgs());
            System.out.println("OSD calculated PG: " + pg);
            ObjectStorageNode node = FileChunkUtils.get_object_primary(oidWitouthVersion, crushMap);

            // TODO improve this logic to use OSD PG's
            if (node != null && node.id == Integer.parseInt(local_id)) {
                System.out.println(local_id + " is primary of PG " + pg);
                for (int i = 0; i < Crush.numberOfReplicas; i++) {
                    boolean success = FileChunkUtils.post_object(oid, data, crushMap, i + 1);
                    if (!success) {
                        responseObserver.onNext(ChunkPostReply.newBuilder()
                                .setState(false)
                                .build());
                        responseObserver.onCompleted();
                        System.out.println("Testing if response observer onCompleted terminates execution.");
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