package storage;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import protos.GetCrushNode;
import protos.GetNodeGrpc;
import protos.SendCrushNode;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CrushNodeClient {
    private static final Logger logger = Logger.getLogger(CrushNodeClient.class.getName());

    private final ManagedChannel channel;
    private final GetNodeGrpc.GetNodeBlockingStub blockingStub;

    public CrushNodeClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }


    CrushNodeClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = GetNodeGrpc.newBlockingStub(channel);
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }


    public void greet(String name) {
        logger.info("Will try to greet " + name + " ...");
        GetCrushNode request = GetCrushNode.newBuilder().setNodeID(123).build();
        SendCrushNode response;
        try {
            response = blockingStub.getNodeWithID(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("NodeID: " + response.getNodeID());
        logger.info("Type: " + response.getType());
        logger.info("Size: " + response.getSize());
        logger.info("AliveSize: " + response.getAliveSize());
        logger.info("IsOsd: " + response.getIsOsd());
        logger.info("Failed: " + response.getFailed());
        logger.info("Overloaded: " + response.getOverloaded());
        logger.info("Children Size: " + response.getChildrenCount());
    }

    public static void main(String[] args) throws Exception {
        CrushNodeClient client = new CrushNodeClient("localhost", 50051);
        try {
            /* Access a service running on the local machine on port 50051 */
            String user = "world";
            if (args.length > 0) {
                user = args[0]; /* Use the arg as the name to greet if provided */
            }
            client.greet(user);
        } finally {
            client.shutdown();
        }
    }
}
