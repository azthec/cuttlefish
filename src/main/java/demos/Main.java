package demos;

import commons.*;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.value.AtomicValue;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import static commons.FileChunkUtils.*;

public class Main {

    private final static String appSvIp = "192.168.1.104";
    private final static String path = "/home/diogo/";

    private static Atomix atomix;
    private static DistributedList<CrushMap> distributed_crush_maps;
    private static AtomicValue<MetadataTree> distributed_metadata_tree;
    private static DistributedLock metaLock;

    public static void main(String[] args) {
        startAtomix();
        test_file_posting();
        test_file_getting();
        test_file_copying();

        atomix.stop();
    }

    public static void startAtomix() {
        AtomixUtils atomixUtils = new AtomixUtils();
        atomix = atomixUtils.getServer("appclient",
                appSvIp, 58007).join();

        distributed_crush_maps = atomix.getList("maps");
        distributed_metadata_tree = atomix.getAtomicValue("mtree");
        metaLock = atomix.getLock("metaLock");
    }

    public static void test_file_splitting() {
        File input = new File(path+"toogood.mp4");
        try {
            byte[][] result = fileToByteArrays(input);

            byteArraysToFile(result, new File(path+"toobyted.mp4"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void test_file_posting() {
        //System.err.close()

      /*  try {
            post_file("example.txt",path, distributed_crush_maps.get(distributed_crush_maps.size()-1),distributed_metadata_tree, metaLock);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        try {
            boolean success = post_file(
                    path+"toogood.mp4",
                    "/folder/tg.mp4",
                    distributed_crush_maps.get(distributed_crush_maps.size() - 1),
                    distributed_metadata_tree,
                    metaLock
            );

            System.out.println("Concluded file posting with success= " + success);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void test_file_getting() {
        //System.err.close();

        MetadataTree meta_tree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file("/folder/tg.mp4", distributed_crush_maps.get(distributed_crush_maps.size() - 1), meta_tree);

        byteArraysToFile(file_bytes, new File(path + "toobad.mp4"));

        System.out.println("Local and remote file hashes:");
        try {
            System.out.println(DigestUtils.sha256Hex(new FileInputStream(path + "toobad.mp4")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        MetadataNode node = distributed_metadata_tree.get().goToNode("/folder/ttgg2.mp4");
        if(node != null)
            System.out.println(node.getHash());
        else
            System.out.println("null");


    }

    public static void test_file_copying() {
        String filePath1 = "/folder/tg.mp4";
        String filePath2 = "/folder/ttgg22.mp4";
        CrushMap crushMap = distributed_crush_maps.get(distributed_crush_maps.size()-1);
        if(FileChunkUtils.copyFile(filePath1, filePath2, "ttgg22.mp4", crushMap, distributed_metadata_tree, metaLock))
            System.out.println("Copied file successfully!");
        else
            System.out.println("Failed to copy file!");


        // must update metadataTree here
        MetadataTree metadataTree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file(filePath2, crushMap, metadataTree);
        byteArraysToFile(file_bytes, new File(path + "toobaddd2.mp4"));

        try {
            System.out.println("Local and remote file hashes:");
            System.out.println(DigestUtils.sha256Hex(new FileInputStream(path + "toobaddd2.mp4")));
            MetadataNode node = metadataTree.goToNode(filePath2);
            if(node != null)
                System.out.println(node.getHash());
            else
                System.out.println("null");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
