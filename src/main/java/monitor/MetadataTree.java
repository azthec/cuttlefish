package monitor;

import java.util.TreeMap;

public class MetadataTree {

    private MetadataNode root;

    public MetadataTree() {
        root = new MetadataNode("root", false, null);
    }

    public MetadataNode get_root() {
        return root;
    }

}
