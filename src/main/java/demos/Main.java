package demos;

import commons.*;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.value.AtomicValue;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static commons.FileChunkUtils.*;

public class Main {
    public static void main(String[] args) {
//        testing();
        test_file_posting();
        test_file_getting();
    }

    public static void test_object_grpc_with_crushmap() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();
        get_object("1337", cluster_map);

        post_object("monsiour", "bogas".getBytes(), cluster_map);

    }

    public static void testing() {
        System.err.close();
        List<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                "192.168.1.100", 5005, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        System.out.println(distributed_metadata_tree.get().goToActualNode("/test.mp4").getObjects());

        atomix.stop();
    }

    public static void test_file_splitting() {
        File input = new File("/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4");
        try {
            byte[][] result = fileToByteArrays(input);

            byteArraysToFile(result, new File("/home/azthec/IdeaProjects/cuttlefish/storage/toobyted.mp4"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void test_file_posting() {
        System.err.close();
        List<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                "192.168.1.100", 5005, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        try {
            MetadataTree meta_tree = distributed_metadata_tree.get();
            post_file(
                    "/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4",
                    "/folder/tg.mp4",
                    distributed_crush_maps.get(0),
                    meta_tree
            );
            distributed_metadata_tree.set(meta_tree);
        } catch (IOException e) {
            e.printStackTrace();
        }

        atomix.stop();
    }

    public static void test_file_getting() {
        System.err.close();
        List<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                "192.168.1.100", 5005, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        MetadataTree meta_tree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file("/folder/tg.mp4", distributed_crush_maps.get(0), meta_tree);

        byteArraysToFile(file_bytes, new File("/home/azthec/IdeaProjects/cuttlefish/storage/toobad.mp4"));

        try {
            System.out.println(DigestUtils.sha256Hex(new FileInputStream("/home/azthec/IdeaProjects/cuttlefish/storage/toobad.mp4")));
            System.out.println(meta_tree.goToActualNode("/folder/tg.mp4").getHash());
        } catch (IOException e) {
            e.printStackTrace();
        }

        atomix.stop();

    }

}
