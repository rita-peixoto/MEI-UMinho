package DataLayer;

import Common.Pair;
import DataLayer.CRDTs.ORSet;
import DataLayer.CRDTs.PCounter;
import DataLayer.CRDTs.PNCounter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static Common.StaticUtilities.readLockWrapping;
import static Common.StaticUtilities.writeLockWrapping;


public class AggregatorData implements DataInterface, NetworkInterface {


    private final String id;
    // The Actual Aggregator Data
    private final Map<String, PCounter<String>> events; // Event =>  PCounter<Aggregator>

    private final ORSet<Device, String> onlineDevices;

    private final PNCounter<String> activeDevices; // PNCounter<Aggregator> (Counts active users != online users)

    private final ReadWriteLock EVNT_lock = new ReentrantReadWriteLock(); // Lock for the events
    private final ReadWriteLock DVC_lock = new ReentrantReadWriteLock(); // Lock for the Online devices
    private final ReadWriteLock ACT_lock = new ReentrantReadWriteLock(); // lock for the active devices

    private final Condition DVC_cond = DVC_lock.writeLock().newCondition(); // Condition variable for updated on the online devices


    public AggregatorData(String id) {
        this.id = id;
        this.events = new HashMap<>();
        this.onlineDevices = new ORSet<>();
        this.activeDevices = new PNCounter<>();

        onlineDevices.registerId(id);
    }

    public String getId() {
        return id;
    }


    public void addEvent(String event) {
        EVNT_lock.writeLock().lock();
        try {
            events.get(event).increment(id);
        } catch (NullPointerException e) {
            events.put(event, new PCounter<>(this.id, 1));
        } finally {
            EVNT_lock.writeLock().unlock();
        }
    }

    public void addOnlineDevice(String user, String type) {
        DVC_lock.writeLock().lock();
        try {
            if (this.onlineDevices.add(Device.of(user, type), id))
                DVC_cond.signalAll();
        } finally {
            DVC_lock.writeLock().unlock();
        }
    }

    public void removeOnlineDevice(String user, String type) {
        DVC_lock.writeLock().lock();
        try {
            if (this.onlineDevices.remove(Device.of(user)))
                DVC_cond.signalAll();
        } finally {
            DVC_lock.writeLock().unlock();
        }
    }

    public void addActiveUser() {
        writeLockWrapping(() -> activeDevices.increment(this.id), ACT_lock);
    }

    public void removeActiveUser() {
        writeLockWrapping(() -> activeDevices.decrement(this.id), ACT_lock);
    }

    /**
     * O número de dispositivos online no sistema;
     */
    public Integer onlineDevices() {
        return readLockWrapping(this.onlineDevices::size, DVC_lock);
    }

    /**
     * Se um dado dispositivo (com um dado id) está online no sistema;
     */
    public Boolean isDeviceOnline(String user) {
        return readLockWrapping(() -> this.onlineDevices.contains(Device.of(user)), DVC_lock);
    }

    /**
     * Se um dado dispositivo (com um dado id) está online na zona;
     */
    public Boolean isDeviceOnlineZone(String user) {
        return readLockWrapping(() -> this.onlineDevices.contains(Device.of(user), this.id), DVC_lock);
    }

    /**
     * O número de dispositivos ativos no sistema; SearchType == ACTIVE
     *
     * @return String reply to be sent to client
     */
    public Integer getActiveDevices() {
        return readLockWrapping(this.activeDevices::value, ACT_lock);
    }

    public Integer getLocalActiveDevices() {
        return readLockWrapping(() -> this.activeDevices.valueOf(this.id), ACT_lock);
    }

    public Map<String, Map<String, Integer>> getEvents() {
        Map<String, Map<String, Integer>> res = new HashMap<>();
        readLockWrapping(() -> this.events.forEach((evn, counter) -> res.put(evn, counter.getCounters())), EVNT_lock);
        return res;
    }

    public Map<String, Integer> getLocalEvents() {
        Map<String, Integer> res = new HashMap<>();
        readLockWrapping(() -> this.events.forEach((evn, counter) -> res.put(evn, counter.valueOf(this.id))), EVNT_lock);
        return res;
    }

    /**
     * O número de eventos de um dado tipo ocorridos no sistema; SearchType == EVENT
     */
    public Integer getEventCount(String event) {
        return readLockWrapping(() -> this.events.getOrDefault(event, new PCounter<>()).value(), EVNT_lock);
    }

    public Integer getLocalEventCount(String event) {
        return readLockWrapping(() -> this.events.getOrDefault(event, new PCounter<>()).valueOf(this.id), EVNT_lock);
    }

    // PUBLISHER-SUBSCRIBER NOTIFICATION SERVICE METHODS

    public Map<String, Long> getLocalTypeCount() {
        return readLockWrapping(
                () -> onlineDevices.elementsOf(this.id).stream().map(Device::type).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())),
                DVC_lock
        );
    }

    public Long getLocalTypeCount(String element) {
        return readLockWrapping(() -> onlineDevices.elementsOf(this.id).stream().map(Device::type).filter(x -> Objects.equals(element, x)).count(), DVC_lock);
    }

    public Map<String, Long> getTypeCount() {
        return readLockWrapping(
                () -> onlineDevices.elements().stream().map(Device::type).collect(Collectors.groupingBy(Function.identity(), Collectors.counting())),
                DVC_lock
        );
    }

    public Long getTypeCount(String element) {
        return readLockWrapping(() -> onlineDevices.elements().stream().map(Device::type).filter(x -> Objects.equals(element, x)).count(), DVC_lock);
    }


    public Integer getLocalOnlineDistribution() {
        return readLockWrapping(() -> this.onlineDevices.size() > 0 ? (int) Math.floorDiv(100 * this.onlineDevices.sizeOf(this.id), this.onlineDevices.size()) : 0, DVC_lock);
    }

    @Override
    public boolean awaitDeviceChange() {
        DVC_lock.writeLock().lock();
        try {
            DVC_cond.await();
            return true;
        } catch (InterruptedException e) {
            return false;
        } finally {
            DVC_lock.writeLock().unlock();
        }
    }

    @Override
    public Snapshot getSnapshot() {
        Map<String, Map<String, Integer>> eventsRelations;
        Map<String, Pair<Integer, Integer>> active;
        Map<Device, Map<String, Integer>> online;
        Map<String, Integer> onlineCounter;
        EVNT_lock.readLock().lock();
        ACT_lock.readLock().lock();
        DVC_lock.readLock().lock();
        try {
            eventsRelations = this.getEvents();
            active = activeDevices.getCounters();
            online = onlineDevices.getOrSet();
            onlineCounter = onlineDevices.getCounters();

        } finally {
            EVNT_lock.readLock().unlock();
            ACT_lock.readLock().unlock();
            DVC_lock.readLock().unlock();

        }
        return new Snapshot(eventsRelations, active, online, onlineCounter);
    }

    @Override
    public Snapshot getNullifyingSnapshot() {
        Pair<Integer, Integer> activeP;
        Map<Device, Map<String, Integer>> online;
        Map<String, Integer> onlineCounter;

        ACT_lock.writeLock().lock();
        DVC_lock.writeLock().lock();
        try {
            activeDevices.removeId(id);
            activeP = activeDevices.getPair(id);
            onlineDevices.removeId(id);
            online = onlineDevices.getOrSet();
            onlineCounter = onlineDevices.getCounters();

        } finally {
            ACT_lock.writeLock().unlock();
            DVC_lock.writeLock().unlock();
        }

        return new Snapshot(Map.of(), activeP == null ? Map.of() : Map.of(id, activeP), online, onlineCounter);
    }

    @Override
    public void applySnapshot(Snapshot snapshot) {

        PNCounter<String> activeCounter = new PNCounter<>(snapshot.active());
        Map<String, PCounter<String>> eventRelations = snapshot.events().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new PCounter<>(x.getValue())));
        ORSet<Device, String> inbound = new ORSet<>(snapshot.online(), snapshot.onlineCounter());

        //State Merge
        writeLockWrapping(() -> eventRelations.forEach((k, v) -> this.events.merge(k, v, PCounter::joinInto)), EVNT_lock);
        writeLockWrapping(() -> this.activeDevices.join(activeCounter), ACT_lock);
        writeLockWrapping(() -> {
            if (this.onlineDevices.join(inbound))
                DVC_cond.signalAll();
        }, DVC_lock);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregatorData that)) return false;
        return id.equals(that.id) && events.equals(that.events) && onlineDevices.equals(that.onlineDevices) && activeDevices.equals(that.activeDevices);
    }

    public boolean equalStates(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregatorData that)) return false;
        return events.equals(that.events) && onlineDevices.equals(that.onlineDevices) && activeDevices.equals(that.activeDevices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, events, onlineDevices, activeDevices);
    }

    @Override
    public String toString() {
        return readLockWrapping(
                () -> "AggregatorData:\n -> id='%s'\n -> events=%s\n -> onlineDevices=%s\n -> activeDevices=%s".formatted(id, events, onlineDevices, activeDevices),
                DVC_lock, EVNT_lock, ACT_lock
        );
    }
}
