package commons;

import io.atomix.core.Atomix;
import io.atomix.core.list.DistributedList;
import io.atomix.core.map.DistributedMap;
import protos.HeartbeatRequest;

import java.util.List;

public class Heartbeat implements Runnable {

    Atomix atomix;
    boolean booting;

    public Heartbeat(Atomix atomix) {
        this.atomix = atomix;
        this.booting = true;
    }


    @Override
    public void run() {
        // TODO may be unecessary, used to stop running on heartbeatmanager creation
        if (booting) {
            booting = false;
            return;
        }
        // TODO change "maps", "objectnodes" and "osd" to read from a config file
        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        if (distributed_crush_maps.size() == 0)
            return;
        DistributedMap<String, ObjectStorageNode> distributed_object_nodes = atomix.getMap("objectnodes");
        if (distributed_object_nodes.size() == 0)
            return;
        List<CrushNode> osds = distributed_crush_maps.get(0).get_nodes_of_type("osd");

        ObjectStorageNode osdn;
        boolean grpc_is_failed;

        // TODO Eventually will have to load from file, use loader.sample_crush_map() for now
        Loader loader = new Loader();
        CrushMap new_map = loader.sample_crush_map();

        boolean changed = false;
        // TODO do this in a non hacky fashion
        for (CrushNode osd : osds) {
            // Execute GRPC heartbeat
            osdn = distributed_object_nodes.get(Integer.toString(osd.nodeID));
            grpc_is_failed = !getHeartbeat(osdn.ip, osdn.port);
            if (grpc_is_failed != osd.isFailed()) {
                changed = true;
            }
            if (grpc_is_failed)
                new_map.get_node_with_id(osd.nodeID).fail();

        }

        // If any node state is changed update the distributed map
        if (changed) {
            new_map.map_epoch = distributed_crush_maps.get(0).map_epoch + 1;
            distributed_crush_maps.add(0, new_map);
        }
//        System.out.println(getHeartbeat("localhost",50051));
    }


    public boolean getHeartbeat(String ip, int port) {
        /**
         * Attempts to hearbeat a gRPC server running on specified ip and port.
         * @return true if running
         */
        HeartbeatClient client = new HeartbeatClient(ip, port);
//        System.out.println("Checking up on node: " + ip + ":" + port);
        HeartbeatRequest request = HeartbeatRequest
                .newBuilder()
                .setStatus(false)
                .build();
        return client.getHeartbeat(request);
    }


}
