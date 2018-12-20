package commons;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CrushNode implements Serializable {
    // uniform buckets don't have weights, I think
    public int nodeID;
    public String type;
    public int size;
    public int alive_size;
    public boolean is_osd;
    public boolean failed;
    public boolean overloaded;

    public CrushNode parent;
    // for now buckets will only contain devices
    private List<CrushNode> children;

    public CrushNode(int nodeID, String type, boolean is_osd, CrushNode parent) {
        this.nodeID = nodeID;
        this.type = type;
        this.is_osd = is_osd;
        this.size = 0;
        this.alive_size = 0;
        children = new ArrayList<>();

        //TODO make dynamic once RADOS is able to track them

        this.failed = false;
        this.overloaded = false;

        this.parent = parent;
    }

    public void add(CrushNode node) {
        if (!is_osd) {
            children.add(node);
            size++;
            if (!node.isFailed() && !node.isOverloaded())
                alive_size++;
        } else {
            throw new IllegalArgumentException("Can't add node to leaf buckets.");
        }
    }

    public void remove(CrushNode node) {
        children.remove(node);
        size--;
        if (!node.isFailed() || !node.isOverloaded()) {
            alive_size--;
        }
    }

    public boolean isFailed() {
        return failed;
    }

    public void fail() {
        this.parent.failChildren(this);
    }

    public void unfail() {
        this.parent.unfailChildren(this);
    }

    private void failChildren(CrushNode node) {
        if (!node.isFailed() && !node.isOverloaded()) {
            node.failed = true;
            alive_size--;
        } else {
            throw new IllegalArgumentException("Node is already failed.");
        }
    }

    private void unfailChildren(CrushNode node) {
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

    public void overload() {
        this.parent.overloadChildren(this);
    }

    public void unoverloadChildren() {
        this.parent.unfailChildren(this);
    }

    public void overloadChildren(CrushNode node) {
        if (!node.isFailed() && !node.isOverloaded()) {
            node.overloaded = true;
            alive_size--;
        } else {
            throw new IllegalArgumentException("Node is already overloaded.");
        }
    }

    public void unoverloadChildren(CrushNode node) {
        if (!node.isFailed() && node.isOverloaded()) {
            node.overloaded = false;
            alive_size++;
        } else {
            throw new IllegalArgumentException("Node is already not overloaded.");
        }
    }

    public List<CrushNode> get_children() {
        return children;
    }

    public List<CrushNode> get_children_of_type(String type) {
        List<CrushNode> filtered_children = new ArrayList<>();
        for (CrushNode child : children) {
            if (child.type.equals(type))
                filtered_children.add(child);
            filtered_children.addAll(child.get_children_of_type(type));
        }
        return filtered_children;
    }

    public void print(int depth) {
        if (depth == 0) {
            System.out.println("└── " + this.nodeID);
        }

        for (Iterator<CrushNode> iterator = this.get_children().iterator(); iterator.hasNext();) {
            CrushNode node = iterator.next();
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
            return "storage." + type + "@" + nodeID;
        }

    }


}
