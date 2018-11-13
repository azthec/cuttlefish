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
    public int alive_size;
    private boolean is_osd;
    public boolean failed;
    public boolean overloaded;

    // for now buckets will only contain devices
    private List<Node> children;

    public Node(int nodeID, String type, boolean is_osd) {
        this.nodeID = nodeID;
        this.type = type;
        this.is_osd = is_osd;
        children = new ArrayList<>();

        //TODO make dynamic once RADOS is able to track them

        this.failed = false;
        this.overloaded = false;
    }

    public void add(Node node) {
        if (!is_osd) {
            children.add(node);
            size++;
            alive_size++;
        } else {
            throw new IllegalArgumentException("Can't add node to leaf buckets.");
        }
    }

    public void remove(Node node) {
        children.remove(node);
        size--;
        if (!node.isFailed()) {
            alive_size--;
        }
    }

    public boolean isFailed() {
        return failed;
    }

    public void failChildren(Node node) {
        if (!node.isFailed() && !node.isOverloaded()) {
            node.failed = true;
            alive_size--;
        } else {
            throw new IllegalArgumentException("Node is already failed.");
        }
    }

    public void unfailChildren(Node node) {
        if (node.isFailed() && !node.isOverloaded()) {
            node.failed = false;
            alive_size++;
        } else {
            throw new IllegalArgumentException("Node is already alive.");
        }
    }

    public boolean isOverloaded() {
        return overloaded;
    }

    public void overloadChildren(Node node) {
        if (!node.isFailed() && !node.isOverloaded()) {
            node.overloaded = true;
            alive_size--;
        } else {
            throw new IllegalArgumentException("Node is already overloaded.");
        }
    }

    public void unoverloadChildren(Node node) {
        if (!node.isFailed() && node.isOverloaded()) {
            node.overloaded = false;
            alive_size++;
        } else {
            throw new IllegalArgumentException("Node is already not overloaded.");
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
