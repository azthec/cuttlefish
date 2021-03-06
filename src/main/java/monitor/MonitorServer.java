package monitor;

import commons.*;


import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.lock.DistributedLock;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.value.AtomicValue;
import io.atomix.primitive.PrimitiveState;
import io.atomix.protocols.raft.MultiRaftProtocol;
import io.atomix.protocols.raft.ReadConsistency;
import org.apache.commons.lang3.RandomStringUtils;


public class MonitorServer {

    public static void main(String[] args) {
        run_raft(args);
    }


    public static void run_raft(String[] args) {
        String local_id = args[0];
        String local_ip = args[1];
        int local_port = Integer.parseInt(args[2]);

        AtomixUtils atomixUtils = new AtomixUtils();
        Atomix atomix = atomixUtils.getServer(local_id, local_ip, local_port).join();

        System.out.println("Created raft group!");
        System.out.println(atomix.getMembershipService().getMembers().toString());

        // Create Raft List of CrushMaps
        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        distributed_crush_maps.addListener(event -> {
            switch (event.type()) {
                case ADD:
                    System.out.println("Entry added: (" + event.element() + ")" +
                            " | @ epoch: " + event.element().map_epoch);
                    event.element().print();
                    break;
                case REMOVE:
                    System.out.println("Entry removed: (" + event.element() +")" +
                            " | @ epoch: " + event.element().map_epoch);
                    event.element().print();
                    break;
            }
        });

        // Create Raft OSD contact information
        DistributedMap<String,ObjectStorageNode> distributed_object_nodes = atomix.getMap("objectnodes");
        // TODO add listeners later

        // Create Raft Metadata file tree
        AtomicValue<MetadataTree> distributed_metadata_tree = atomix.getAtomicValue("mtree");
        distributed_metadata_tree.addListener(event -> {
            switch (event.type()) {
                case UPDATE:
                    System.out.println("Metadata tree updated: (" + event.newValue() + ")");
                    event.newValue().print();
                    break;
            }
        });

        // Create Raft Metadata tree lock
        // TODO test that tryLock does return false when it can't acquire the lock
        DistributedLock metaLock = atomix.getLock("metaLock");
        metaLock.addStateChangeListener(event -> {
            if (event == PrimitiveState.CONNECTED) {
                System.out.println("MetatreeLock connected!");
            } else if (event == PrimitiveState.SUSPENDED) {
                System.out.println("MetatreeLock suspended!");
            } else if (event == PrimitiveState.EXPIRED) {
                System.out.println("MetatreeLock expired!");
            } else if (event == PrimitiveState.CLOSED) {
                System.out.println("MetatreeLock closed!");
            }
        });


        Loader loader = new Loader();
        if (local_id.equals("figo")) {
            if (distributed_crush_maps.size() <= 0) {
                register_map(distributed_crush_maps, loader.sample_crush_map());
                register_object_nodes(distributed_object_nodes, Loader.sample_osds());
            }
            if (distributed_metadata_tree.get() == null) {
                MetadataTree metadataTree = loader.sample_metadata_tree();
                metadataTree.initializePgs(Loader.getTotalPgs());
                distributed_metadata_tree.set(metadataTree);
                // distributed_metadata_tree.set(new MetadataTree());
            }
            atomix.lockBuilder("metaLock").build();
            metaLock = atomix.getLock("metaLock");
            metaLock.unlock();
        } else {
            // wait for figo to register initial map
            System.out.println("Waiting for distributed primitives initial setup.");
            while(distributed_crush_maps.size() == 0
                    && distributed_metadata_tree.get() == null) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // should be uneeded, but atomix sometimes glitches out on locks
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        distributed_crush_maps.get(distributed_crush_maps.size() - 1).print();
        distributed_metadata_tree.get().print();
        if (local_id == "figo")
            System.out.println("metaLock isLocked: " + metaLock.isLocked());


//        System.out.println(distributed_metadata_tree.get().getPgs());


        // TODO uncomment this when implementing OSD failure tolerance
        // If RAFT leader manage OSD hearbeats and update CrushMap
        // adapts to RAFT leader changes automatically
//        try {
//            // give time for OSD's to boot
//            TimeUnit.SECONDS.sleep(30);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

         ScheduledFuture<?> heartbeat_manager = register_heartbeat_manager(atomix, local_id, "system");



    }

    public static ScheduledFuture<?> register_heartbeat_manager(Atomix atomix, String local_id, String pg_name) {
        System.out.println("Registering heartbeat manager!");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeat_manager =  executorService.scheduleAtFixedRate(
                new HeartbeatManager(atomix, local_id, pg_name),
                0, 5,
                TimeUnit.SECONDS);
        return heartbeat_manager;
    }


    public static void register_map(DistributedList<CrushMap> distributed_crush_maps,
                                    CrushMap crush_map) {
        int epoch;
        if (distributed_crush_maps.size() >= 1) {
            epoch = distributed_crush_maps.get(distributed_crush_maps.size() - 1).map_epoch + 1;
        } else {
            epoch = 0;
        }
        crush_map.map_epoch = epoch;
        distributed_crush_maps.add(crush_map);
    }


    public static void register_object_nodes(DistributedMap<String,ObjectStorageNode> distributed_object_nodes,
                                             List<ObjectStorageNode> object_nodes) {
        for (ObjectStorageNode node : object_nodes) {
            distributed_object_nodes.put("" + node.id, node);
        }
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

}
