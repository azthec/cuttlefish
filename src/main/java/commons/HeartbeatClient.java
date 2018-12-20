package commons;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import protos.GetHeartbeatGrpc;
import protos.HeartbeatReply;
import protos.HeartbeatRequest;


import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class HeartbeatClient {
    /*
    General gRPC client code section
     */

    private static final Logger logger = Logger.getLogger(HeartbeatClient.class.getName());

    private final ManagedChannel channel;
    private final GetHeartbeatGrpc.GetHeartbeatBlockingStub blockingStub;

    public HeartbeatClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }


    HeartbeatClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = GetHeartbeatGrpc.newBlockingStub(channel);
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }



    /*
    Custom gRPC client code section
     */

    public boolean getHeartbeat(HeartbeatRequest request) {
        // TODO eventually check up on overloaded boolean too
        HeartbeatReply response;

        try {
            response = blockingStub.getHeartbeat(request);
        } catch (StatusRuntimeException e) {
//            System.out.println("Node is unreachable.");
            return false;
        }
        return true;
    }
}
