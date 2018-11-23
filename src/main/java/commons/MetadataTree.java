package commons;


public class MetadataTree {

    private MetadataNode root;

    public MetadataTree() {
        root = new MetadataNode("root", MetadataNode.FOLDER, null);
    }

    public MetadataNode get_root() {
        return root;
    }

    public void print() {
        root.print(0);
    }
}
