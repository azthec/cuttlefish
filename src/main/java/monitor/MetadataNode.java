package monitor;

import java.util.*;

// potentially uneeded
public class MetadataNode implements Comparable<MetadataNode> {

    MetadataNode parent;
    String name;
    String path;
    boolean is_leaf;
    List<MetadataNode> children;

    public MetadataNode(String name, boolean is_leaf,MetadataNode parent) {
        this.parent = parent;
        this.is_leaf = is_leaf;
        this.name = name;
        if (!is_leaf) {
            name = name + "/";
        }
        if (parent != null) {
            this.path = parent.getPath() + name;
        } else {
            this.path = "/";
        }

        children = new ArrayList<>();
    }

    public void add(String name, boolean is_leaf) {
        if (get(name) == null) {
            this.children.add(new MetadataNode(name, is_leaf, this));
        } else {
            throw new IllegalArgumentException("A file with the same name already exists!");
        }
    }

    public MetadataNode get(String name) {
        for (MetadataNode child : children) {
            if (child.name.equals(name)) {
                return child;
            }
        }
        return null;
    }

    public void remove(String name) {
        MetadataNode child = get(name);
        if (child != null) {
            children.remove(child);
        }
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(MetadataNode otherNode) {
        return this.path.compareTo(otherNode.getPath());
    }
}
