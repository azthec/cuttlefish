package commons;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import protos.ChunkData;
import protos.ChunkOid;
import protos.ChunkPostReply;
import protos.ChunkTransferGrpc;


import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class FileChunkClient {
    /*
    General gRPC client code section
     */

    private static final Logger logger = Logger.getLogger(FileChunkClient.class.getName());

    private final ManagedChannel channel;
    private final ChunkTransferGrpc.ChunkTransferBlockingStub blockingStub;
    private final ChunkTransferGrpc.ChunkTransferStub asyncStub;

    public FileChunkClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }


    FileChunkClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = ChunkTransferGrpc.newBlockingStub(channel);
        asyncStub = ChunkTransferGrpc.newStub(channel);
    }


    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }



    /*
    Custom gRPC client code section
     */

    // returns byte array of data
    public byte[] getChunk(String oid) {
        ChunkOid request = ChunkOid
                .newBuilder()
                .setOid(oid)
                .build();
        ChunkData response;
        byte[] data = {};

        try {
            response = blockingStub.getChunk(request);
            return response.getData().toByteArray();

        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus());
        }
        return null;
    }

    public boolean postChunk(String oid, byte[] chunks) {
        ChunkPostReply response;

        try {
            response = blockingStub.postChunk(ChunkData.newBuilder()
                    .setOid(oid)
                    .setData(ByteString.copyFrom(chunks))
                    .setReplication(true)
                    .build());
            return response.getState();

        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus());
        }

        return false;
    }

    public boolean postChunkOSD(String oid, byte[] chunks) {
        ChunkPostReply response;

        try {
            response = blockingStub.postChunk(ChunkData.newBuilder()
                    .setOid(oid)
                    .setData(ByteString.copyFrom(chunks))
                    .setReplication(false)
                    .build());
            return response.getState();

        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus());
        }

        return false;
    }
}
