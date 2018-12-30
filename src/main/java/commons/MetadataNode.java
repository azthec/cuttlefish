package commons;

import java.util.*;

// Indispensable
public class MetadataNode implements Comparable<MetadataNode> {

    static boolean FILE = false;
    static boolean FOLDER = true;

    private MetadataNode parent;
    private String name;
    private String path;
    private boolean type;
    private int version;
    private List<MetadataNode> children;
    private int numberOfChunks;
    private List<MetadataChunk> chunks;
    private String hash;

    public MetadataNode getParent() {
        return parent;
    }

    public void setParent(MetadataNode parent) {
        this.parent = parent;
        this.path = parent.getPath() + name;
    }

    public String getName() {
        return name;
    }

    public boolean isFolder() {
        return type;
    }

    public boolean isFile() {
        return !type;
    }

    public boolean isLeaf() {
        return children.size() <= 0;
    }

    public String getPath() {
        return path;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<MetadataNode> getChildren() {
        return children;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<String> getChunksOidList() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < numberOfChunks; i++) {
            result.add(path + "_" + i + "_" + version);
        }
        return result;
    }

    public void setNumberOfChunks(int numberOfChunks) {
        if (type != FOLDER) {
            this.numberOfChunks = numberOfChunks;
        } else {
            throw new IllegalArgumentException("Folders can't have contents.");
        }
    }

    public List<MetadataChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<MetadataChunk> chunks) {
        this.chunks = chunks;
    }

    public void addChunk(MetadataChunk chunk) {
        this.chunks.add(chunk);
    }

    MetadataNode(String name, boolean type, MetadataNode parent) {
        this.parent = parent;
        this.type = type;
        this.name = name;
        // append a slash to folders (in path only)
        if (type) {
            name = name + "/";
        }
        if (parent != null) {
            this.path = parent.getPath() + name;
        } else {
            this.path = "/";
        }

        children = new ArrayList<>();
        chunks = new ArrayList<>();
    }

    public MetadataNode addChild(MetadataNode child) {
        // Be careful when using this function to obey node addition rules
        // parent is set to this node when added and path adjusted, existing child is replaced
        child.setParent(this);
        remove(child.getName());
        this.children.add(child);
        return child;

    }

    public MetadataNode addFile(String name) {
        if (get(name) == null) {
            MetadataNode child = new MetadataNode(name, FILE, this);
            this.children.add(child);
            return child;
        } else {
            throw new IllegalArgumentException("An entry with the same name already exists!");
        }
    }

    public MetadataNode addFolder(String name) {
        if (get(name) == null) {
            MetadataNode child = new MetadataNode(name, FOLDER, this);
            this.children.add(child);
            return child;
        } else {
            throw new IllegalArgumentException("An entry with the same name already exists!");
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

    @Override
    public String toString() {
        return path;
    }

    @Override
    public int compareTo(MetadataNode otherNode) {
        return this.path.compareTo(otherNode.getPath());
    }

    public void print(int depth) {
        if (depth == 0) {
            System.out.println("└── " + this.name);
        }

        for (Iterator<MetadataNode> iterator = this.children.iterator(); iterator.hasNext();) {
            String spaces = String.format("%"+ (depth + 4) +"s", "");
            MetadataNode node = iterator.next();
            String slash = "";
            if (node.isFolder())
                slash = "/";

            if (iterator.hasNext()) {
                System.out.println(spaces + "├── " + node.name + slash);
            } else {
                System.out.println(spaces + "└── " + node.name + slash);
            }
            if (!node.isLeaf()) {
                node.print(depth + 4);
            }
        }
    }
}
