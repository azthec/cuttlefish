package monitor;

import commons.AtomixUtils;
import commons.CrushMap;
import commons.CrushNode;
import commons.Crush;


import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;


import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;


public class MonitorServer {
    public static void main(String[] args) {
        run_raft(args);
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

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer(local_id, local_ip, local_port, servers).join();

        System.out.println("Created raft group!");
        System.out.println(atomix.getMembershipService().getMembers().toString());

        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        distributed_crush_maps.addListener(event -> {
            switch (event.type()) {
                case ADD:
                    System.out.println("Entry added: (" + event.element() + ")");
                    break;
                case REMOVE:
                    System.out.println("Entry removed: (" + event.element() +")");
                    break;
            }
        });


        if (local_id.equals("figo")) {
            // TODO replace this with loading a config file eventually
            register_map(distributed_crush_maps, sample_crush_map());
        }

        // wait for figo to register initial map
        while(distributed_crush_maps.size() == 0) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Scanner in = new Scanner(System.in);

        while (true) {
            try {
                CrushMap shared_crush_map = distributed_crush_maps.get(0);
                System.out.println("Current map: " + shared_crush_map +
                        " | @ epoch: " + shared_crush_map.map_epoch);
                shared_crush_map.get_root().print(0);
                if (local_id.equals("figo")) {
                    shared_crush_map.get_root().add(new CrushNode(in.nextInt(), "row", false));
                    register_map(distributed_crush_maps, shared_crush_map);
                }
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void register_map(DistributedList<CrushMap> distributed_crush_maps,
                                    CrushMap crush_map) {
        int previous_epoch = 0;
        if (distributed_crush_maps.size() >= 1) {
            previous_epoch = distributed_crush_maps.get(0).map_epoch;
        }
        crush_map.map_epoch = previous_epoch + 1;
        distributed_crush_maps.add(0, crush_map);
    }


    public static void register_osd() {

    }

    public static void update_osd() {

    }

    public static CrushMap sample_crush_map() {
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
        return cluster_map;
    }

    public static void crush_poc() {

        CrushMap cluster_map = sample_crush_map();

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
