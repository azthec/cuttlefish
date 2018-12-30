package commons;


import java.util.*;

public class MetadataTree {

    private MetadataNode root;

    public HashMap<Integer, MetadataPlacementGroup> getPgs() {
        return pgs;
    }

    public boolean addObjectToPg(int pg, String oid) {
        return pgs.get(pg).getObjects().add(oid);
    }

    public void initializePgs(int totalPgs) {
        // only use on first ever bootup
        pgs = new HashMap<>();
        for (int i = 0; i < totalPgs; i++) {
            pgs.put(
                    i,
                    new MetadataPlacementGroup(i, 0, new HashSet<>())
            );
        }
    }

    private HashMap<Integer, MetadataPlacementGroup> pgs;

    // this only occurs on first total system boot, hence the epoch at 0 and empty oid set
    public MetadataTree() {
        root = new MetadataNode("root", MetadataNode.FOLDER, null);
    }

    public MetadataNode get_root() {
        return root;
    }

    public void print() {
        root.print(0);
    }

    public boolean nodeExists(String path) {
        MetadataNode node = goToNode(path);
        return node != null;
    }

    /**
     * Travels to the parent folder of a given node's path
     * @param path is the path to the node
     * @return the MetadataNode resulting from goToNode
     */
    public MetadataNode goToParentFolder(String path) {
        int p=path.lastIndexOf("/");
        String parentPath=path.substring(0, p+1);
        return goToNode(parentPath);
    }

    /**
     * Travels the MetadataTree following:
     * @param path the path given
     * @return a MetadataNode, which can be null, if the path makes no sense given the tree.
     */
    public MetadataNode goToNode(String path) {
        List<String> pathSplit = new LinkedList<>(Arrays.asList(path.split("/")));
        if (path.equals("/")) {
            return root;
        }
        MetadataNode node = root;
        pathSplit.remove(0);
        for (String next : pathSplit) {
            MetadataNode nextNode = node.get(next);
            if (nextNode == null)
                return null;
            if (nextNode.getPath().equals(path))
                return nextNode;
            node = nextNode;
        }
        return null;
    }

    /**
     * Method that goes down the tree given the current absolute path for a client
     * @param path the current absolute path fo the client
     * @return the node where the client is at that moment
     */

    public MetadataNode goToNode(MetadataNode startingNode, String path){
        List<String> pathSplit = new LinkedList<>(Arrays.asList(path.split("/")));
        if (path.equals("/")) {
            return root;
        }
        MetadataNode node = startingNode;
        for (String next : pathSplit) {
            MetadataNode nextNode = node.get(next);
            if (nextNode == null)
                return null;
            if (nextNode.getPath().equals(path))
                return nextNode;
            node = nextNode;
        }
        return null;
    }

}
