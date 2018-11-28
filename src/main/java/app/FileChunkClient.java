package app;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.ArrayUtils;
import protos.ChunkData;
import protos.ChunkOid;
import protos.ChunkPostReply;
import protos.ChunkTransferGrpc;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static commons.Utils.splitArray;


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
    public byte[] getChunk(ChunkOid request) {
        Iterator<ChunkData> response;
        byte[] data = {};

        try {
            response = blockingStub.getChunk(request);
            for (int i = 1; response.hasNext(); i++) {
                ChunkData chunk = response.next();
                data = ArrayUtils.addAll(data, chunk.getData().toByteArray());
                System.out.println(chunk.getData());
            }

            return data;

        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus());
        }
        return null;
    }

    public void postChunk(byte[] chunks, String oid) throws InterruptedException {
        List<ChunkPostReply> values = new ArrayList<>();

        StreamObserver<ChunkPostReply> responseObserver = new StreamObserver<ChunkPostReply>() {
            @Override
            public void onNext(ChunkPostReply value) {
                System.out.println(value);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
            }
        };


        StreamObserver<ChunkData> requestObserver = asyncStub.postChunk(responseObserver);

        try {
            byte[][] chunks_matrix = splitArray(chunks, 2);
            System.out.println("Number of chunks: " + chunks_matrix.length);
                for (byte[] chunk : chunks_matrix) {
                    ChunkData data = ChunkData.newBuilder()
                            .setData(ByteString.copyFrom(chunk))
                            .setOid(oid)
                            .build();
                    System.out.println(data);
                    requestObserver.onNext(data);
                }

        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }
        // Mark the end of requests
        requestObserver.onCompleted();

    }
}
