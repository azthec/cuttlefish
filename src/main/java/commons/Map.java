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

/*
 * The cluster map is composed of devices and buckets, both of
 * which have numerical identifiers and weight values associated
 * with them.
 *
 * Devices are always at the leaves.
 */
public class Map {
    public int map_epoch;
    public Node root;
    // n_placement_groups = 2k − 1
    public int n_placement_groups;
    // TODO CRUSH hierarchy
    // TODO CRUSH placement rules

    public Map() {
        map_epoch = 0;
        root = new Node(000, "root", -1, false);
    }

    public Node get_root() {
        return root;
    }



}