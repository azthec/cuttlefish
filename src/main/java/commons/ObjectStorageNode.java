package commons;

public class ObjectStorageNode {
    public int id;
    public String ip;
    public int port;

    public ObjectStorageNode(int id, String ip, int port) {
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
