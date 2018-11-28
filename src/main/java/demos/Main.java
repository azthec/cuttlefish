package demos;

import app.FileChunkClient;
import protos.ChunkOid;

import java.io.UnsupportedEncodingException;

public class Main {
    public static void main(String[] args) {
        getChunk("localhost", 50420);
    }

    public static void getChunk(String ip, int port) {
        FileChunkClient client = new FileChunkClient(ip, port);


        System.out.println("Getting!");
        ChunkOid request = ChunkOid
                .newBuilder()
                .setOid("1337")
                .build();
        client.getChunk(request);

        System.out.println("Posting!");
        byte[] post_it = new byte[0];
        try {
            post_it = "1234567890".getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            client.postChunk(post_it, "1337");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
