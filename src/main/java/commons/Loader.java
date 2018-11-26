package commons;

import java.util.ArrayList;
import java.util.List;

public class Loader {
    /**
     * This class will eventually hold the methods to load config from files
     *
     */

    public CrushMap sample_crush_map() {
        // nodes have hardcoded ips for now
        CrushMap cluster_map = new CrushMap();

        CrushNode b101 = new CrushNode(101, "row",false, cluster_map.get_root());
        b101.add(new CrushNode(0, "osd", true, b101));
        b101.add(new CrushNode(1, "osd", true, b101));
        b101.add(new CrushNode(2, "osd", true, b101));
        CrushNode b102 = new CrushNode(102, "row", false, cluster_map.get_root());
        b102.add(new CrushNode(3, "osd", true, b102));
        b102.add(new CrushNode(4, "osd", true, b102));
        b102.add(new CrushNode(5, "osd", true, b102));

        // overload some dudes
        // b001.overloadChildren(b001.get_children().get(0));
        // b001.overloadChildren(b001.get_children().get(2));
        // b001.overloadChildren(b010.get_children().get(3));
        // b111.failChildren(b111.get_children().get(2));

        cluster_map.get_root().add(b101);
        cluster_map.get_root().add(b102);

        return cluster_map;
    }

    public List<ObjectStorageNode> sample_osds() {
        // Hardcoded ip ports for now
        List<ObjectStorageNode> osds = new ArrayList<>();
        osds.add(new ObjectStorageNode(0, "localhost", 50420));
        osds.add(new ObjectStorageNode(1, "localhost", 50421));
        osds.add(new ObjectStorageNode(2, "localhost", 50422));
        osds.add(new ObjectStorageNode(3, "localhost", 50423));
        osds.add(new ObjectStorageNode(4, "localhost", 50424));
        osds.add(new ObjectStorageNode(5, "localhost", 50425));
        return osds;
    }
}
