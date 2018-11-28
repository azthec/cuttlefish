package storage;

import com.google.protobuf.ByteString;
import commons.CrushNode;
import commons.FileUtils;
import commons.Heartbeat;
import io.atomix.protocols.raft.protocol.HeartbeatResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jetty.util.ArrayUtil;
import protos.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static commons.FileUtils.read_file_to_byte_array;


public class GRPCServer {
    // TODO eventually read from config, in megabytes
    // 1024 * 1024 * 10
    public static final int CHUNK_SIZE = 10;

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
        final GRPCServer server = new GRPCServer();
        server.start(Integer.parseInt(args[0]));
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

        // see https://grpc.io/docs/tutorials/basic/java.html#server-side-streaming-rpc
        @Override
        public void getChunk(ChunkOid request, StreamObserver<ChunkData> responseObserver) {
            // TODO test gRPC chunks
            // returning hardcoded list for tests
            System.out.println("Serving chunk!");
//            List<ChunkData> chunks = new ArrayList<>();
//            chunks.add(ChunkData
//                    .newBuilder()
//                    .setOid("123")
//                    .setData(ByteString.copyFromUtf8("1337"))
//            .build());
//            chunks.add(ChunkData
//                    .newBuilder()
//                    .setOid("3456")
//                    .setData(ByteString.copyFromUtf8("7331"))
//                    .build());
//            for (ChunkData chunk : chunks) {
//                    responseObserver.onNext(chunk);
//            }
            try {
                System.out.println("Reading file!");
                System.out.println(new String(read_file_to_byte_array(request.getOid())));
            } catch (IOException e) {
                e.printStackTrace();
            }

            responseObserver.onCompleted();
        }

        // see https://grpc.io/docs/tutorials/basic/java.html#client-side-streaming-rpc
        @Override
        public StreamObserver<ChunkData> postChunk(StreamObserver<ChunkPostReply> responseObserver) {
            System.out.println("Receiving chunk!");
            return new StreamObserver<ChunkData>() {
                private byte[] data = {};
                String oid;

                @Override
                public void onNext(ChunkData chunk) {
                    System.out.println("appending chunk!" + chunk);
                    oid = chunk.getOid();
                    data = ArrayUtils.addAll(data, chunk.getData().toByteArray());
                }

                @Override
                public void onError(Throwable t) {
                    responseObserver.onError(t);
                }

                @Override
                public void onCompleted() {
                    System.out.println("Length of data: " + data.length);
                    // Once we received everything we save it to a file named chunk.oid
                    FileUtils.write_byte_array_to_file(data, oid);
                    responseObserver.onNext(ChunkPostReply.newBuilder()
                            .setState(true)
                            .build());
                }
            };
        }

    }
}