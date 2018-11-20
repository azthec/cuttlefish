package monitor;

import commons.CrushMap;
import commons.CrushNode;
import commons.Crush;

import java.io.File;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


import io.atomix.core.Atomix;
import io.atomix.core.AtomixBuilder;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.map.AtomicMap;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import io.atomix.protocols.raft.partition.RaftPartitionGroup;
import io.atomix.storage.StorageLevel;
import io.atomix.utils.net.Address;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;


public class MonitorServer {
    public static void main(String[] args) {
//        run_raft(args);
//        crush_poc();
    }


    public static void run_raft(String[] args) {
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
        System.out.println(atomix.getMembershipService().getMembers().toString());

        // try to share a fucking map
        AtomicMap<Object, Object> map = atomix.atomicMapBuilder("map")
                .withNullValues()
                .withCacheEnabled()
                .withCacheSize(100)
                .build();

        map.addListener(event -> {
            switch (event.type()) {
                case INSERT:
                    System.out.println("Entry added: (" + event.key() +
                            "," + event.newValue().value() + ")");
                    break;
                case UPDATE:
                    System.out.println("Entry updated: (" + event.key() +
                            "," + event.oldValue().value() + ") -> (" + event.key() +
                            "," + event.newValue().value() + ")");
                    break;
                case REMOVE:
                    System.out.println("Entry removed: (" + event.key() +
                            "," + event.newValue().value() + ")");
                    break;
            }
        });

        DistributedLock lock = atomix.lockBuilder("lockerooni")
                .withProtocol(MultiRaftProtocol.builder()
                        .withReadConsistency(ReadConsistency.LINEARIZABLE)
                        .build())
                .build();

        if (local_id.equals("figo")) {
            try {
                map.put("Hello", "World!");
                lock.lock();
                System.out.println(lock.isLocked());
            } catch (io.atomix.primitive.PrimitiveException e) {
                e.printStackTrace();
            }
        }

        Scanner in = new Scanner(System.in);

        while (true) {
            try {
                if (local_id.equals("figo")) {
                    map.put(in.nextLine(), in.nextLine());
                }
                System.out.println(lock.isLocked());
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

        atomix.getMembershipService().addListener(event -> System.out.println(event.toString()));

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
        CrushMap cluster_map = new CrushMap();
        Random random = new Random();

        CrushNode b101 = new CrushNode(101, "row",false);
        b101.add(new CrushNode(0, "osd", true));
        b101.add(new CrushNode(1, "osd", true));
        b101.add(new CrushNode(2, "osd", true));
        CrushNode b102 = new CrushNode(102, "row", false);
        b102.add(new CrushNode(3, "osd", true));
        b102.add(new CrushNode(4, "osd", true));
        b102.add(new CrushNode(5, "osd", true));


        // overload some dudes
        // b001.overloadChildren(b001.get_children().get(0));
        // b001.overloadChildren(b001.get_children().get(2));
        // b001.overloadChildren(b010.get_children().get(3));
        // b111.failChildren(b111.get_children().get(2));

        cluster_map.get_root().add(b101);
        cluster_map.get_root().add(b102);

        cluster_map.get_root().print(0);

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

}
