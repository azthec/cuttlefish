package monitor;

import commons.Map;
import commons.Node;
import commons.Crush;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.map.AtomicMap;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;


public class MonitorServer {
    public static void main(String[] args) {
        String local_id = args[0];
        String local_ip = args[1];
        int local_port = Integer.parseInt(args[2]);
        // Raft requires a static membership list
        List<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");

        Atomix atomix = getServer(local_id, local_ip, local_port, servers).join();

        System.out.println("Created raft group!");

        // try to share a fucking map
        AtomicMap<Object, Object> map = atomix.atomicMapBuilder("map")
                .withNullValues()
                .withCacheEnabled()
                .withCacheSize(100)
                .build();

        map.addListener(event -> {
            switch (event.type()) {
                case INSERT:
                    System.out.println("Insert event.");
                    break;
                case UPDATE:
                    System.out.println("Update event.");
                    break;
                case REMOVE:
                    System.out.println("Remove event.");
                    break;
            }
        });

        if (local_id.equals("figo")) {
            try {
                map.put("hello", "world");
            } catch (io.atomix.primitive.PrimitiveException e) {
                e.printStackTrace();
            }
        }

        Scanner in = new Scanner(System.in);

        while (true) {
            try {
                System.out.println(map.get("hello"));
                if (local_id.equals("figo")) {
                    map.put(in.nextLine(), in.nextLine());
                }
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }



    }

    public static CompletableFuture<Atomix> getServer(String local_id, String local_ip,
                                                      int local_port,
                                                      List<String> servers) {
        AtomixBuilder builder = Atomix.builder();
        builder.withMemberId(local_id)
                .withAddress(local_ip, local_port)
                .withMulticastEnabled()
                .withMulticastAddress(new Address("230.4.20.69", 8008))
                .withManagementGroup(RaftPartitionGroup.builder("system")
                        .withNumPartitions(1).withMembers(servers)
                        .withDataDirectory(new File("mngdir", local_id))
                        .withStorageLevel(StorageLevel.MEMORY).build())
                .addPartitionGroup(RaftPartitionGroup.builder("data")
                        .withNumPartitions(1)
                        .withDataDirectory(new File("datadir", local_id))
                        .withStorageLevel(StorageLevel.MEMORY)
                        .withMembers(servers)
                        .build());
        Atomix atomix = builder.build();

        // atomix.getMembershipService().addListener(event -> System.out.println(event.toString()));

        System.out.println("Starting node: " + local_id + " @ Port: " + local_port + ".");
        return CompletableFuture.supplyAsync(() -> {
            atomix.start().join();
            return atomix;
        });
    }


    public static void register_osd() {

    }

    public static void update_osd() {

    }

    public static void crush_poc() {
        commons.Map cluster_map = new Map();
        Random random = new Random();

        Node b001 = new Node(110, "row",false);
        b001.add(new Node(0, "osd", true));
        b001.add(new Node(1, "osd", true));
        b001.add(new Node(2, "osd", true));
        b001.add(new Node(3, "osd", true));
        Node b010 = new Node(111, "row", false);
        b010.add(new Node(4, "osd", true));
        b010.add(new Node(5, "osd", true));
        b010.add(new Node(6, "osd", true));
        b010.add(new Node(7, "osd", true));
        Node b100 = new Node(112, "row", false);
        b100.add(new Node(8, "osd", true));
        b100.add(new Node(9, "osd", true));
        b100.add(new Node(10, "osd", true));
        b100.add(new Node(11, "osd", true));
        Node b111 = new Node(112, "row", false);
        b111.add(new Node(12, "osd", true));
        b111.add(new Node(13, "osd", true));
        b111.add(new Node(14, "osd", true));
        b111.add(new Node(15, "osd", true));

        // overload some dudes
        // b001.overloadChildren(b001.get_children().get(0));
        // b001.overloadChildren(b001.get_children().get(2));
        // b001.overloadChildren(b001.get_children().get(3));
        // b111.failChildren(b111.get_children().get(2));

        cluster_map.get_root().add(b001);
        cluster_map.get_root().add(b010);
        cluster_map.get_root().add(b100);
        cluster_map.get_root().add(b111);

        cluster_map.get_root().print(0);

        Crush crush = new Crush();

        test_select_randomness(crush, cluster_map.get_root());


        // System.out.println(crush.select_OSDs(cluster_map.get_root(), "1337"));
    }

    public static void test_select_randomness(Crush crush, Node root) {
        int[] counters = new int[16];
        List<Node> git = new ArrayList<>();
        List<Node> root_list = new ArrayList<>();
        root_list.add(root);
        for (int i = 0; i<1000000; i++) {
            String oid = RandomStringUtils.random(32);
            String sha256hex = DigestUtils.sha256Hex(oid);
            BigInteger oid_bint = new BigInteger(sha256hex, 16);
            List<Node> got = crush.select(3, "row", root_list, oid_bint);
            git = crush.select(1, "osd", got, oid_bint);
            for (Node j : git) {
                counters[j.nodeID]++;
            }
        }
        System.out.println(Arrays.toString(counters));
    }

}
