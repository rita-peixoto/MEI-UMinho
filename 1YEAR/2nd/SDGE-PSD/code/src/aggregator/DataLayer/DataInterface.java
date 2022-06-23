package DataLayer;

import java.util.Map;

public interface DataInterface {

    /**
     * @return The id of the node
     */
    String getId();

    void addOnlineDevice(String username, String type);

    void addActiveUser();

    void removeActiveUser();

    void removeOnlineDevice(String username, String type);

    void addEvent(String token);

    Integer onlineDevices();

    Boolean isDeviceOnline(String user);

    Boolean isDeviceOnlineZone(String user);

    Integer getActiveDevices();

    Integer getLocalActiveDevices();

    Map<String, Map<String, Integer>> getEvents();

    Map<String, Integer> getLocalEvents();

    Integer getEventCount(String event);

    Integer getLocalEventCount(String event);

    Map<String, Long> getLocalTypeCount();


    Long getLocalTypeCount(String element);

    Map<String, Long> getTypeCount();

    Long getTypeCount(String element);

    Integer getLocalOnlineDistribution();

    boolean awaitDeviceChange();
}
