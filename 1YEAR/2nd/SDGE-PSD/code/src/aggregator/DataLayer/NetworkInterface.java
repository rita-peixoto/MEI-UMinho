package DataLayer;

public interface NetworkInterface {

    Snapshot getSnapshot();

    Snapshot getNullifyingSnapshot();

    void applySnapshot(Snapshot snapshot);


}
