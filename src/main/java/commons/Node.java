package commons;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class Node {
    // uniform buckets don't have weights, I think
    public int weight = 0;
    public String alg = "CRUSH_BUCKET_UNIFORM";
    public int nodeID;
    public String type;
    public int size;
    public boolean is_osd;
    public boolean failed;
    public boolean overloaded;

    // for now buckets will only contain devices
    private List<Node> children;

    public Node(int nodeID, String type, int size, boolean is_osd, boolean failed) {
        this.nodeID = nodeID;
        this.type = type;
        this.size = 4;
        this.is_osd = is_osd;
        children = new ArrayList<>();

        //TODO make dynamic once RADOS is able to track them

        this.failed = failed;
        this.overloaded = false;
    }

    public void add(Node node) {
        if (!is_osd) {
            children.add(node);
        } else {
            throw new IllegalArgumentException("Can't add node to leaf buckets.");
        }
    }

    public List<Node> get_children() {
        return children;
    }

    public void print(int depth) {
        if (depth == 0) {
            System.out.println("└── " + this.nodeID);
        }

        for (Iterator<Node> iterator = this.get_children().iterator(); iterator.hasNext();) {
            Node node = iterator.next();
            String spaces = String.format("%"+ (depth + 4) +"s", "");
            String fail = "";
            if (node.overloaded) {
                fail = "*";
            } else if (node.failed) {
                fail = "!";
            }
            if (iterator.hasNext()) {
                System.out.println(spaces + "├── " + node.nodeID + fail);
            } else {
                System.out.println(spaces + "└── " + node.nodeID + fail);
            }
            if (! node.is_osd) {
                node.print(depth + 4);
            }
        }
    }

    public String toString() {
        if (!is_osd) {
            return "bucket." + type + "@" + nodeID;
        } else {
            return "osd." + type + "@" + nodeID;
        }

    }


}
