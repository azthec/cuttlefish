package demos;

import com.google.gson.Gson;
import commons.*;
import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.value.AtomicValue;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static commons.FileChunkUtils.*;
import static monitor.PersistentStorage.*;

public class Main {
    public static void main(String[] args) {
//        test_loader();
//        testing();
        test_file_posting();
        // if creating new atomix node with same name needs to wait for timeout
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        test_file_getting();
//        test_serialize();
    }

    public static void crush_poc() {
        Loader loader = new Loader();
        CrushMap cluster_map = loader.sample_crush_map();

        Crush crush = new Crush();

        // test_select_randomness(crush, cluster_map.get_root());
        // System.out.println(crush.select_OSDs(cluster_map.get_root(), "1337"));

        // this maps an object to a placement group
        int total_pgs = 255;
        int pg = Crush.get_pg_id("1337", total_pgs);
        System.out.println("PG: " + pg);

        // this maps a placement group to OSD's
        System.out.println(crush.select_OSDs(cluster_map.get_root(), "" + pg));

    }

    public static void test_select_randomness(Crush crush, CrushNode root) {
        int[] counters = new int[6];
        List<CrushNode> git = new ArrayList<>();
        List<CrushNode> root_list = new ArrayList<>();
        root_list.add(root);
        for (int i = 0; i<1000000; i++) {
            String oid = RandomStringUtils.random(32);
            BigInteger oid_bint = Crush.hash(oid);
            List<CrushNode> got = crush.select(2, "row", root_list, oid_bint);
            git = crush.select(1, "osd", got, oid_bint);
            for (CrushNode j : git) {
                counters[j.nodeID]++;
            }
        }
        System.out.println(Arrays.toString(counters));
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
                "192.168.1.65", 5005, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        System.out.println(distributed_metadata_tree.get().goToNode("/test.mp4").getNumberOfChunks());

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
                "192.168.1.65", 5010, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");
        
        try {
            MetadataTree meta_tree = distributed_metadata_tree.get();
            boolean success = post_file(
                    "/home/azthec/IdeaProjects/cuttlefish/storage/toogood.mp4",
                    "/folder/tg.mp4",
                    distributed_crush_maps.get(0),
                    meta_tree
            );
            if (success)
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
                "192.168.1.65", 5010, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        MetadataTree meta_tree = distributed_metadata_tree.get();

        byte[][] file_bytes = get_file("/folder/tg.mp4", distributed_crush_maps.get(0), meta_tree);

        byteArraysToFile(file_bytes, new File("/home/azthec/IdeaProjects/cuttlefish/storage/toobad.mp4"));

        try {
            System.out.println(DigestUtils.sha256Hex(new FileInputStream("/home/azthec/IdeaProjects/cuttlefish/storage/toobad.mp4")));
            System.out.println(meta_tree.goToNode("/folder/tg.mp4").getHash());
        } catch (IOException e) {
            e.printStackTrace();
        }

        atomix.stop();
    }

    public static void test_serialize() {
//        System.err.close();
        List<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer("appclient",
                "192.168.1.65", 5010, servers).join();

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");

        try {
            storeMaps(Loader.loadPersistentStoragePath(), distributed_crush_maps);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ArrayList<CrushMap> loaded_maps = null;
        try {
            loaded_maps = loadMaps(Loader.loadPersistentStoragePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (loaded_maps != null) {
            loaded_maps.get(0).print();
        }

        try {
            storeMetadata(Loader.loadPersistentStoragePath(), distributed_metadata_tree);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        MetadataTree loaded_mdata = null;
        try {
            loaded_mdata = loadMetadata(Loader.loadPersistentStoragePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (loaded_mdata != null) {
            loaded_mdata.print();
        }

        atomix.stop();
    }

    public  static void test_loader() {
        System.out.println(Loader.loadPersistentStoragePath());
    }

}
