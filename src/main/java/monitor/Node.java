package monitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Node {
    // uniform buckets don't have weights, I think
    int weight = 0;
    String alg = "CRUSH_BUCKET_UNIFORM";
    int hash;
    String type;
    int size;
    boolean is_leaf;
    boolean failed;
    boolean overloaded;

    // for now buckets will only contain devices
    private List<Node> children;

    public Node(int hash, String type, int size, boolean is_leaf) {
        this.hash = hash;
        this.type = type;
        this.size = size;
        this.is_leaf = is_leaf;
        children = new ArrayList<>();

        //TODO remove once CRUSH is updated with failed and overloaded functions
        this.failed = false;
        this.overloaded = false;
    }

    void add(Node node) {
        if (! is_leaf) {
            children.add(node);
        } else {
            throw new IllegalArgumentException("Can't add node to leaf buckets.");
        }
    }

    List<Node> get_children() {
        return children;
    }

    void print(int depth) {
        if (depth == 0) {
            System.out.println("└── " + this.hash);
        }

        for (Iterator<Node> iterator = this.get_children().iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            String spaces = String.format("%"+ (depth + 4) +"s", "");
            if (iterator.hasNext()) {
                System.out.println(spaces + "├── " + node.hash);
            } else {
                System.out.println(spaces + "└── " + node.hash);
            }
            if (! node.is_leaf) {
                node.print(depth + 4);
            }
        }
    }


}
