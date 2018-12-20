package commons;


/*
 * RADOS ensures a consistent view of the data distribution and
 * consistent read and write access to data objects through the
 * use of a versioned cluster map.
 *
 * Each OSD includes a CPU, some volatile RAM, a network interface,
 * and a locally attached disk drive or RAID. Monitors are
 * stand-alone processes and require a small amount of local storage
 *
 * The cluster map specifies cluster membership, device state, and
 * the mapping of data objects to devices. The data distribution
 * is specified first by mapping objects to placement groups
 * (controlled by m) and then mapping each PG onto a set of
 * devices (CRUSH).
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * The cluster map is composed of devices and buckets, both of
 * which have numerical identifiers and weight values associated
 * with them.
 *
 * Devices are always at the leaves.
 */
public class CrushMap implements Serializable {
    public int map_epoch;
    public int total_pgs;
    public CrushNode root;
    // TODO CRUSH hierarchy
    // TODO CRUSH placement rules

    // Cluster OSD's, addresses and current state
    public List<CrushNode> cluster_members;


    public CrushMap() {
        map_epoch = 0;
        total_pgs = 255;
        root = new CrushNode(000, "root", false, null);
    }

    public CrushNode get_root() {
        return root;
    }

    public CrushNode get_node_with_id(int id) {
        for (CrushNode child : root.get_children()) {
            if (child.nodeID == id)
                return child;
        }
        return null;
    }

    public List<CrushNode> get_nodes_of_type(String type) {
        return root.get_children_of_type(type);
    }

    public void print() {
        root.print(0);
    }



}