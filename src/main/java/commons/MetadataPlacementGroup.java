package commons;

import java.util.HashSet;

public class MetadataPlacementGroup {
    private int id;
    private int lastCompleteRepeerEpoch;
    private HashSet<String> objects;

    @Override
    public String toString() {
        return "MetadataPlacementGroup{" +
                "id=" + id +
                ", lastCompleteRepeerEpoch=" + lastCompleteRepeerEpoch +
                ", objects=" + objects +
                '}';
    }

    public boolean active(int currentCrushMapEpoch) {
        return lastCompleteRepeerEpoch == currentCrushMapEpoch;
    }

    public int getLastCompleteRepeerEpoch() {
        return lastCompleteRepeerEpoch;
    }

    public void setLastCompleteRepeerEpoch(int lastCompleteRepeerEpoch) {
        this.lastCompleteRepeerEpoch = lastCompleteRepeerEpoch;
    }

    public HashSet<String> getObjects() {
        return objects;
    }

    public void setObjects(HashSet<String> objects) {
        this.objects = objects;
    }

    public MetadataPlacementGroup(int id, int lastCompleteRepeerEpoch, HashSet<String> objects) {
        this.id = id;
        this.lastCompleteRepeerEpoch = lastCompleteRepeerEpoch;
        this.objects = objects;
    }
}
