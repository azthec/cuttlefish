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
        // TODO may be unecessary, remove later date
        if (booting) {
            booting = false;
            return;
        }
        // TODO change "maps" and "osd" to read from a config file
        DistributedList<CrushMap> distributed_crush_maps = atomix.getList("maps");
        if (distributed_crush_maps.size() == 0)
            return;
        DistributedMap<String, ObjectStorageNode> distributed_object_nodes = atomix.getMap("objectnodes");
        if (distributed_object_nodes.size() == 0)
            return;
        List<CrushNode> osds = distributed_crush_maps.get(0).get_nodes_of_type("osd");

        ObjectStorageNode osdn;
        boolean failed_state;
        // TODO Eventually will have to load from file, use loader.sample_crush_map() for now
        Loader loader = new Loader();
        CrushMap new_map = loader.sample_crush_map();
        List<CrushNode> new_osds = new_map.get_nodes_of_type("osd");
        boolean changed = false;
        // TODO do this in a non hacky fashion
        for (CrushNode osd : osds) {
            // Execute GRPC heartbeat
            osdn = distributed_object_nodes.get(Integer.toString(osd.nodeID));
            failed_state = !getHeartbeat(osdn.ip, osdn.port);
            if (failed_state != osd.isFailed()) {
                changed = true;
            }

        }
        for (CrushNode osd: new_osds) {
            osdn = distributed_object_nodes.get(Integer.toString(osd.nodeID));
            failed_state = !getHeartbeat(osdn.ip, osdn.port);
            if (failed_state)
                osd.parent.failChildren(osd);
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
