package commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Loader {
    /**
     * This class will eventually hold the methods to load config from files
     *
     */

    final static String figoIP = "10.132.0.2:5000";
    final static String messiIP = "10.132.0.3:5000";
    final static String ronaldoIP = "10.132.0.4:5000";
    final static String osd1IP="10.132.0.5";
    final static String osd2IP="10.132.0.6";
    final static String osd3IP="10.132.0.7";
    final static String osd4IP="10.132.0.8";
    final static String osd5IP="10.132.0.9";
    final static String osd6IP="10.132.0.10";


    public static CrushMap sample_crush_map() {
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
//        b101.get_children().get(0).fail();
//        b101.get_children().get(2).fail();
//        b102.get_children().get(2).overload();

        cluster_map.get_root().add(b101);
        cluster_map.get_root().add(b102);

        return cluster_map;
    }

    public static List<String> loadServerNames(){
        ArrayList<String> servers = new ArrayList<>();
        servers.add("figo");
        servers.add("messi");
        servers.add("ronaldo");
        return servers;
    }

    public static HashMap<String, String> sample_monitors() {
        HashMap<String, String> res = new HashMap<>();
        res.put("figo", figoIP);
        res.put("messi", messiIP);
        res.put("ronaldo", ronaldoIP);
        /*res.put("figo", "192.168.1.100:5000");
        res.put("messi", "192.168.1.100:5001");
        res.put("ronaldo", "192.168.1.100:5002");*/
        return res;
    }

    public static List<ObjectStorageNode> sample_osds() {
        // Hardcoded ip ports for now
        List<ObjectStorageNode> osds = new ArrayList<>();
        osds.add(new ObjectStorageNode(0, osd1IP ,50420));
        osds.add(new ObjectStorageNode(1, osd2IP, 50420));
        osds.add(new ObjectStorageNode(2, osd3IP, 50420));
        osds.add(new ObjectStorageNode(3, osd4IP ,50420));
        osds.add(new ObjectStorageNode(4, osd5IP, 50420));
        osds.add(new ObjectStorageNode(5, osd6IP, 50420));
//        osds.add(new ObjectStorageNode(0, "192.168.1.100" ,50420));
//        osds.add(new ObjectStorageNode(1, "192.168.1.100", 50421));
//        osds.add(new ObjectStorageNode(2, "192.168.1.100", 50422));
//        osds.add(new ObjectStorageNode(3, "192.168.1.100" ,50423));
//        osds.add(new ObjectStorageNode(4, "192.168.1.100", 50424));
//        osds.add(new ObjectStorageNode(5, "192.168.1.100", 50425));
        return osds;
    }

    static public HashMap<String, ObjectStorageNode> get_osd_map() {
        // be careful, ordering in hashmaps is not guaranteed!
        HashMap<String, ObjectStorageNode> hashMap = new HashMap<>();
        for (ObjectStorageNode osd : sample_osds()) {
            hashMap.put("" + osd.id, osd);
        }
        return hashMap;
    }

    public ObjectStorageNode get_osd_with_id(int id) {
        for (ObjectStorageNode osd : sample_osds()) {
            if (osd.id == id)
                return osd;
        }
        return null;
    }

    public MetadataTree sample_metadata_tree() {
        MetadataTree metadata_tree = new MetadataTree();
        MetadataNode root = metadata_tree.get_root();
        root.addFolder("folder");
        return metadata_tree;
    }

    public static int getTotalPgs() {
        return 255;
    }
}
