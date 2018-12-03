package commons;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

    public MetadataNode goToNode(String path){
        List<String> pathSplit = new LinkedList<>(Arrays.asList(path.split("/")));
        MetadataNode node = root;
        while(!pathSplit.isEmpty()){
            String next = pathSplit.remove(0);
            MetadataNode nextNode = node.get(next);
            if(nextNode != null && nextNode.isFolder()){
                node = nextNode;
            }
        }
        return node;
    }

    /**
     * Method that goes down the tree given the current absolute path for a client
     * @param path the current absolute path fo the client
     * @return the node where the client is at that moment
     */
    public MetadataNode goToNode(MetadataNode startingNode, String path){
        List<String> pathSplit = new LinkedList<>(Arrays.asList(path.split("/")));
        MetadataNode node = startingNode;
        while(!pathSplit.isEmpty()){
            String next = pathSplit.remove(0);
            MetadataNode nextNode = node.get(next);
            if(nextNode != null && nextNode.isFolder()){
                node = nextNode;
            }
        }
        return node;
    }

}
