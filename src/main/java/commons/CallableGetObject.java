package commons;

import java.util.concurrent.Callable;

import static commons.FileChunkUtils.get_object_primary;
import static commons.FileChunkUtils.get_random_object_osd;

public class CallableGetObject implements Callable<byte[]> {
    private MetadataChunk metadataChunk;
    private CrushMap crushMap;

    public CallableGetObject(MetadataChunk metadataChunk, CrushMap crushMap) {
        this.metadataChunk = metadataChunk;
        this.crushMap = crushMap;
    }

    @Override
    public byte[] call() {
        System.out.println("Getting object with ID: " + metadataChunk.getChunkOidVersion());

//        ObjectStorageNode node = get_object_primary(metadataChunk.getChunkOid(), crushMap);
        ObjectStorageNode node = get_random_object_osd(metadataChunk.getChunkOid(), crushMap);
        if (node == null) {
            System.out.println("Failed to find node for object: " + metadataChunk.getChunkOidVersion());
            return new byte[0];
        }
        System.out.println("Getting from node: " + node.id);
        FileChunkClient client = new FileChunkClient(node.ip, node.port);
        byte [] get_result = client.getChunk(metadataChunk.getChunkOidVersion());
        if (get_result == null || get_result.length <= 0) {
            System.out.println("File getting failed");
            return new byte[0];
        }

        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return get_result;
    }
}
