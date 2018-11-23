package storage;

import commons.CrushNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import protos.CrushNodeRequest;
import protos.GetNodeGrpc;
import protos.CrushNodeReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class CrushNodeServer {
    private static final Logger logger = Logger.getLogger(CrushNodeServer.class.getName());

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GetNodeGrpcImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                CrushNodeServer.this.stop();
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
        final CrushNodeServer server = new CrushNodeServer();
        server.start();
        server.blockUntilShutdown();
    }


    static CrushNodeReply CrushNodeObjectToCrushNodeReply(CrushNode node) {
        CrushNodeReply.Builder builder = CrushNodeReply
                .newBuilder()
                .setNodeID(node.nodeID)
                .setType(node.type)
                .setSize(node.size)
                .setAliveSize(node.alive_size)
                .setIsOsd(node.is_osd)
                .setFailed(node.failed)
                .setOverloaded(node.overloaded);
        List<CrushNodeReply> children = new ArrayList<>();
        for( CrushNode child : node.get_children()) {
            children.add(CrushNodeObjectToCrushNodeReply(child));
        }
        return builder.addAllChildren(children).build();
    }


    static class GetNodeGrpcImpl extends GetNodeGrpc.GetNodeImplBase {

        @Override
        public void getNode(CrushNodeRequest req, StreamObserver<CrushNodeReply> responseObserver) {
            System.out.println("Running getNodeWithID of class GetNodeGrpcImpl!");
            CrushNode node = new CrushNode(111, "osd", false);
            responseObserver.onNext(CrushNodeObjectToCrushNodeReply(node));
            responseObserver.onCompleted();
        }

    }
}