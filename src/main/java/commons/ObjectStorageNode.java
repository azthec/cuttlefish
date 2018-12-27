package commons;

public class ObjectStorageNode {
    public String id;
    public String ip;
    public int port;

    public ObjectStorageNode(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return "ObjectStorageNode{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
