package monitor;

import commons.*;


import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.value.AtomicValue;
import org.apache.commons.lang3.RandomStringUtils;

import static monitor.PersistentStorage.*;


public class MonitorServer {

    public static void main(String[] args) {
        run_raft(args);
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

        if(servers.size() < 3) {
            System.err.println("Program requires at least three members to function.");
            System.exit(1);
        }

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
                    try {
                        storeMaps(Loader.loadPersistentStoragePath(), distributed_crush_maps);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Failed to save metadata update!");
                    }
                    break;
                case REMOVE:
                    // Realistically, cmaps should never be removed.
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
                    try {
                        storeMetadata(Loader.loadPersistentStoragePath(), distributed_metadata_tree);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Failed to save metadata update!");
                    }
                    break;
            }
        });


        // -- Persistent boot logic --

        // Load stored data
        ArrayList<CrushMap> storedCrushMaps;
        MetadataTree storedMetadataTree;
        try {
            storedCrushMaps = getPersistentMaps(Loader.loadPersistentStoragePath());
            storedMetadataTree = getPersistentMetadata(Loader.loadPersistentStoragePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load persistent data!");
            atomix.stop();
            return;
        }


        // Create access to clusters cmap and mdata stored versions
        // TODO change boot logic to support dynamic number and naming of servers (# >= 3)
        DistributedMap<String, Integer> mdataVers = atomix.getMap("mdataVers");
        DistributedMap<String, Integer> cmapsVers = atomix.getMap("cmapsVers");

        mdataVers.put(local_id, storedCrushMaps.size());
        cmapsVers.put(local_id, storedMetadataTree.epoch);

        while(distributed_crush_maps.size() == 0 && distributed_metadata_tree.get() == null) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }



        Loader loader = new Loader();
        if (local_id.equals("figo")) {
            if (distributed_crush_maps.size() <= 0) {
                // TODO replace this with loading a config file
                register_map(distributed_crush_maps, loader.sample_crush_map());
                register_object_nodes(distributed_object_nodes, loader.sample_osds());
            }
            // TODO replace this with initialize metadata tree from disk
            if (distributed_metadata_tree.get() == null) {
                distributed_metadata_tree.set(loader.sample_metadata_tree());
                // distributed_metadata_tree.set(new MetadataTree());
            }
        } else {
            // wait for figo to register initial map
            System.out.println("Waiting for distributed primitives initial setup.");
            while(distributed_crush_maps.size() == 0 && distributed_metadata_tree.get() == null) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        distributed_crush_maps.get(0).print();

        // TODO uncomment this when implementing OSD failure tolerance
        // If RAFT leader manage OSD hearbeats and update CrushMap
        // adapts to RAFT leader changes automatically
        // ScheduledFuture<?> heartbeat_manager = register_heartbeat_manager(atomix, local_id, "system");



    }

    public static ScheduledFuture<?> register_heartbeat_manager(Atomix atomix, String local_id, String pg_name) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeat_manager =  executorService.scheduleAtFixedRate(
                new HeartbeatManager(atomix, local_id, pg_name),
                0, 5,
                TimeUnit.SECONDS);
        return heartbeat_manager;
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


    public static void register_object_nodes(DistributedMap<String,ObjectStorageNode> distributed_object_nodes,
                                             List<ObjectStorageNode> object_nodes) {
        for (ObjectStorageNode node : object_nodes) {
            distributed_object_nodes.put(Integer.toString(node.id), node);
        }
    }

}
