package storage;

import commons.CrushNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import protos.GetCrushNode;
import protos.GetNodeGrpc;
import protos.SendCrushNode;

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


    static SendCrushNode sendCrushNodify(CrushNode node) {
        SendCrushNode.Builder builder = SendCrushNode
                .newBuilder()
                .setNodeID(node.nodeID)
                .setType(node.type)
                .setSize(node.size)
                .setAliveSize(node.alive_size)
                .setIsOsd(node.is_osd)
                .setFailed(node.failed)
                .setOverloaded(node.overloaded);
        List<SendCrushNode> children = new ArrayList<>();
        for( CrushNode child : node.get_children()) {
            children.add(sendCrushNodify(child));
        }
        return builder.addAllChildren(children).build();
    }


    static class GetNodeGrpcImpl extends GetNodeGrpc.GetNodeImplBase {

        @Override
        public void getNodeWithID(GetCrushNode req, StreamObserver<SendCrushNode> responseObserver) {
            System.out.println("Running getNodeWithID of class GetNodeGrpcImpl!");
            CrushNode node = new CrushNode(111, "osd", false);
            responseObserver.onNext(sendCrushNodify(node));
            responseObserver.onCompleted();
        }

    }
}