package DataLayer;

import Common.Pair;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static Common.StaticUtilities.*;

public record Snapshot(Map<String, Map<String, Integer>> events,
                       Map<String, Pair<Integer, Integer>> active,
                       Map<Device, Map<String, Integer>> online,
                       Map<String, Integer> onlineCounter) implements Serializable {
    public static Snapshot deserialize(List<byte[]> frames) {
        List<String> aggregators = deserializeList(frames.get(0), String::new);
        List<String> events = deserializeList(frames.get(1), String::new);
        List<String> types = deserializeList(frames.get(2), String::new);
        List<Device> devices = deserializeList(frames.get(3), x -> Device.deserialize(types::get, x));
        ByteBuffer countersArray = ByteBuffer.wrap(frames.get(4));
        ByteBuffer onlineMatrix = ByteBuffer.wrap(frames.get(5));
        ByteBuffer activeUsrArray = ByteBuffer.wrap(frames.get(6));
        ByteBuffer relationMatrix = ByteBuffer.wrap(frames.get(7));

        //Deserialization
        //Active Devices Counter
        Map<String, Pair<Integer, Integer>> active = aggregators.stream().collect(Collectors.toMap(x -> x, x -> Pair.of(activeUsrArray.getInt(), activeUsrArray.getInt())));
        active.entrySet().stream().filter(x -> x.getValue().apply((a, b) -> b == 0 && a == 0)).toList().forEach(x -> active.remove(x.getKey()));

        //System-Events
        Map<String, Map<String, Integer>> eventRelations = new HashMap<>();
        for (String e : events) {
            Map<String, Integer> m = aggregators.stream().collect(Collectors.toMap(a -> a, a -> relationMatrix.getInt()));
            m.entrySet().stream().filter(x -> x.getValue() == 0).map(Map.Entry::getKey).toList().forEach(m::remove);
            eventRelations.put(e, m);
        }

        // Online Devices
        Map<Device, Map<String, Integer>> online = new HashMap<>();
        for (Device d : devices) {
            int count = onlineMatrix.getInt();
            if (count > 0) online.put(d, new HashMap<>());
            for (int i = 0; i < count; i++) {
                int kIndex = onlineMatrix.getInt();
                int value = onlineMatrix.getInt();
                online.get(d).put(aggregators.get(kIndex), value);
            }
        }
        //Aggregator Online Counter
        Map<String, Integer> onlineCounter = aggregators.stream().map(x -> Map.entry(x, countersArray.getInt())).collect(KVStreamToMap());


        return new Snapshot(eventRelations, active, online, onlineCounter);
    }

    public List<byte[]> serialize() {
        // Aggregator Ids
        List<String> aggregators = this.onlineCounter.keySet().stream().toList();
        Map<String, Integer> aggMap = IntStream.range(0, aggregators.size()).boxed().collect(Collectors.toMap(aggregators::get, i -> i));
        // Events Ids
        List<String> events = this.events.keySet().stream().toList();
        //Types
        List<String> types = online.keySet().stream().map(Device::type).collect(Collectors.toSet()).stream().toList();
        Map<String, Integer> typeMap = IntStream.range(0, types.size()).boxed().collect(Collectors.toMap(types::get, i -> i));
        //Devices
        List<Device> devices = this.online.keySet().stream().toList();

        //Online Counters
        ByteBuffer countersArray = ByteBuffer.allocate(aggregators.size() * 4);
        aggregators.forEach(agg -> countersArray.putInt(onlineCounter.get(agg)));

        //Online Relations
        ByteBuffer onlineMatrix = ByteBuffer.allocate(online.values().stream().map(x -> x.size() * 8).reduce(online.size() * 4, Integer::sum));
        devices.stream().map(online::get)
                .forEach(dotSet -> {
                    onlineMatrix.putInt(dotSet.size());
                    dotSet.forEach((k, v) -> onlineMatrix.putInt(aggMap.get(k)).putInt(v));
                });
        //Active Users PNCounter

        ByteBuffer activeUsrArray = ByteBuffer.allocate(aggregators.size() * 8);
        aggregators.stream().map(active::get).forEach(p -> {
            if (p == null)
                activeUsrArray.putInt(0).putInt(0);
            else
                activeUsrArray.putInt(p.getFirst()).putInt(p.getSecond());
        });

        // Relations
        ByteBuffer relationMatrix = ByteBuffer.allocate(aggregators.size() * events.size() * 4);
        events.forEach(event -> aggregators.forEach(agg -> relationMatrix.putInt(this.events.get(event).getOrDefault(agg, 0))));


        //Framing
        List<byte[]> frames = new Vector<>(8);

        frames.add(serializeList(aggregators, String::getBytes));
        frames.add(serializeList(events, String::getBytes));
        frames.add(serializeList(types, String::getBytes));
        frames.add(serializeList(devices, d -> d.serialize(typeMap::get)));
        frames.add(countersArray.array());
        frames.add(onlineMatrix.array());
        frames.add(activeUsrArray.array());
        frames.add(relationMatrix.array());

        return frames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Snapshot snapshot)) return false;
        return events.equals(snapshot.events) && active.equals(snapshot.active) && online.equals(snapshot.online) && onlineCounter.equals(snapshot.onlineCounter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(events, active, online, onlineCounter);
    }

    @Override
    public String toString() {
        return "Snapshot:\n -> events=%s\n -> active=%s\n -> online=%s\n -> onlineCounter=%s".formatted(events, active, online, onlineCounter);
    }
}
