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

    private final static String appSvIp = "10.132.0.11"; //"192.168.1.104";
    private final static String path = "/home/diogo/";
    private static String remoteFilePath1 = "/folder/tg.mp4";
    private static String remoteFilePath2 = "/folder/ttgg22.mp4";

    private static Atomix atomix;
    private static DistributedList<CrushMap> distributed_crush_maps;
    private static AtomicValue<MetadataTree> distributed_metadata_tree;
    private static DistributedLock metaLock;

    public static void main(String[] args) {

        System.out.println("STARTING TEST");
        startAtomix();
        long starttime = System.currentTimeMillis();
        test_file_posting();
        long posttime =  System.currentTimeMillis() - starttime;
        test_file_getting();
        long gettime = System.currentTimeMillis() - posttime;
        test_file_copying();
        long copytime = System.currentTimeMillis() - gettime;

        atomix.stop();
        System.out.println("512MB");
        System.out.println("posttime: "+posttime);
        System.out.println("gettime: "+gettime);
        System.out.println("copytime: "+copytime);
        System.out.println("---------------------------");

    }

    public static void post_string_to_file() {
        String textToPost = "copy pasterino oparino doparino";
        try {
            boolean success = post_bytes(textToPost.getBytes(),
                    remoteFilePath1,
                    distributed_crush_maps.get(distributed_crush_maps.size() - 1),
                    distributed_metadata_tree,
                    metaLock
            );

            System.out.println("Concluded file posting with success= " + success);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    path+"512MB.zip",
                    remoteFilePath1,
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

        byte[][] file_bytes = get_file(remoteFilePath1, distributed_crush_maps.get(distributed_crush_maps.size() - 1), meta_tree);

        byteArraysToFile(file_bytes, new File(path + "toobad.mp4"));

        System.out.println("Local and remote file hashes:");
        try {
            System.out.println(DigestUtils.sha256Hex(new FileInputStream(path + "toobad.mp4")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        MetadataNode node = distributed_metadata_tree.get().goToNode(remoteFilePath1);
        if(node != null)
            System.out.println(node.getHash());
        else
            System.out.println("null");


    }

    public static void test_file_copying() {
        CrushMap crushMap = distributed_crush_maps.get(distributed_crush_maps.size()-1);
        if(FileChunkUtils.copyFile(remoteFilePath1, remoteFilePath2, "ttgg22.mp4", crushMap, distributed_metadata_tree, metaLock))
            System.out.println("Copied file successfully!");
        else
            System.out.println("Failed to copy file!");


        // must update metadataTree here
        MetadataTree metadataTree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file(remoteFilePath2, crushMap, metadataTree);
        byteArraysToFile(file_bytes, new File(path + "toobaddd2.mp4"));

        try {
            System.out.println("Local and remote file hashes:");
            System.out.println(DigestUtils.sha256Hex(new FileInputStream(path + "toobaddd2.mp4")));
            MetadataNode node = metadataTree.goToNode(remoteFilePath2);
            if(node != null)
                System.out.println(node.getHash());
            else
                System.out.println("null");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
