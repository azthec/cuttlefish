package demos;

import commons.*;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.value.AtomicValue;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static commons.FileChunkUtils.*;

public class Main {

    private final static String appSvIp = "192.168.1.65";
    private final static String path = "/home/diogo/";

    public static void main(String[] args) {
//        testing();
//        test_file_posting();
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        test_file_getting();
    }

    public static void test_object_grpc_with_crushmap() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
//        get_object("1337", cluster_map);
//
//        post_object("monsiour", "bogas".getBytes(), cluster_map);

    }

    public static void testing() {
        //System.err.close();

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                appSvIp, 5005).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        System.out.println(distributed_metadata_tree.get().goToNode("/test.mp4").getNumberOfChunks());

        atomix.stop();
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
        //System.err.close();

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                appSvIp, 5010).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");
        DistributedLock metaLock = atomix.getLock("metaLock");

        try {
            boolean success = post_file(
                    path+"toogood.mp4",
                    "/folder/tg.mp4",
                    distributed_crush_maps.get(0),
                    distributed_metadata_tree,
                    metaLock
            );
            System.out.println("Concluded file posting with success= " +success);
        } catch (IOException e) {
            e.printStackTrace();
        }

        atomix.stop();
    }

    public static void test_file_getting() {
        //System.err.close();

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                appSvIp, 5010).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        MetadataTree meta_tree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file("/folder/tg.mp4", distributed_crush_maps.get(0), meta_tree);

        byteArraysToFile(file_bytes, new File(path + "toobad.mp4"));

        try {
            System.out.println(DigestUtils.sha256Hex(new FileInputStream(path + "toobad.mp4")));
            System.out.println(meta_tree.goToNode("/folder/tg.mp4").getHash());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(distributed_metadata_tree.get().getPgs());

        atomix.stop();
    }

}
