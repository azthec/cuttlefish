package storage;

import commons.MetadataChunk;
import commons.MetadataNode;
import commons.MetadataTree;
import io.atomix.core.value.AtomicValue;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.listFiles;

public class CleanupRunnable implements Runnable {
    String datafolder;
    AtomicValue<MetadataTree> distributedMetadataTree;

    public CleanupRunnable(String datafolder, AtomicValue<MetadataTree> distributedMetadataTree) {
        this.datafolder = datafolder;
        this.distributedMetadataTree = distributedMetadataTree;
    }

    @Override
    public void run() {
        // TODO locks and stuff
        System.out.println("Running cleanup routine.");

        MetadataTree metadataTree = distributedMetadataTree.get();
        File datafolderFile = new File(datafolder);
        for(File file : listFiles(datafolderFile, null, true)) {
            String chunkOid = file.getPath().substring(datafolder.length()-1);
//            System.out.println(chunkOid);
            int chunkVersion = Integer.parseInt(
                    chunkOid.substring(chunkOid.lastIndexOf("_")).substring(1)
            );
            chunkOid = chunkOid.substring(0, chunkOid.lastIndexOf("_"));
            int chunkPart = Integer.parseInt(
                    chunkOid.substring(chunkOid.lastIndexOf("_")).substring(1)
            );
            chunkOid = chunkOid.substring(0, chunkOid.lastIndexOf("_"));
//            System.out.println("Oid: " + chunkOid);
//            System.out.println("Part#: " + chunkPart);
//            System.out.println("Version: " + chunkVersion);
            MetadataNode node = metadataTree.goToNode(chunkOid);
            if(node != null) {
                // TODO improve deletion logic for chunks
                if (node.getVersion() > chunkVersion) {
                    System.out.println("Deleting: " + chunkOid + "_" + chunkPart + "_" + chunkVersion);
                    file.delete();
                }
            } else {
                // this should never occur, as nodes are never really removed from meta tree
                System.out.println("Node dosen't exist, deleting: " + chunkOid + "_" + chunkPart + "_" + chunkVersion);
                file.delete();
            }
        }
    }
}
