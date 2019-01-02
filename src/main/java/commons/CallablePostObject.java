package commons;

import java.util.concurrent.Callable;

import static commons.FileChunkUtils.get_object_primary;

public class CallablePostObject implements Callable<Boolean> {
    private MetadataChunk metadataChunk;
    private byte[] data;
    private CrushMap crushMap;

    public CallablePostObject(MetadataChunk metadataChunk, byte[] data, CrushMap crushMap) {
        this.metadataChunk = metadataChunk;
        this.data = data;
        this.crushMap = crushMap;
    }

    @Override
    public Boolean call() {
        System.out.println("Posting object with ID: " + metadataChunk.getChunkOidVersion());

        ObjectStorageNode primary = get_object_primary(metadataChunk.getChunkOid(), crushMap);
        if (primary == null) {
            System.out.println("Failed to find primary for object: " + metadataChunk.getChunkOidVersion());
            return false;
        }

        FileChunkClient client = new FileChunkClient(primary.ip, primary.port);
        boolean post_result = client.postChunk(metadataChunk.getChunkOidVersion(), data);
        System.out.println("File posting returned: " + post_result + "\n");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return post_result;
    }
}
