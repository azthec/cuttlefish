package commons;

public class MetadataChunk {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public String getChunkOid() {
        return file_path + "_" + id;
    }

    public String getChunkOidVersion() {
        return file_path + "_" + id + "_" + version;
    }

    private int id;
    private int version;
    private String hash;
    private String file_path;

    @Override
    public String toString() {
        return "MetadataChunk{" +
                "id=" + id +
                ", version=" + version +
                ", hash='" + hash + '\'' +
                ", file_path='" + file_path + '\'' +
                '}';
    }

    MetadataChunk(int id, int version, String hash, String file_path) {
        this.id = id;
        this.version = version;
        this.hash = hash;
        this.file_path = file_path;
    }
}
