package DataLayer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public record Device(String name, String type) {


    public Device(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Device(String name) {
        this(name, null);
    }

    public static Device of(String username, String type) {
        return new Device(username, type);
    }

    public static Device of(String username) {
        return new Device(username);
    }

    public static Device deserialize(Function<Integer, String> typeIndexer, byte[] bytes) {
        var b = ByteBuffer.wrap(bytes);
        String type = typeIndexer.apply(b.getInt());

        return new Device(new String(Arrays.copyOfRange(bytes, 4, bytes.length)), type);
    }

    public byte[] serialize(Function<String, Integer> typeIndexer) {
        byte[] n = name.getBytes();
        return ByteBuffer.allocate(4 + n.length).putInt(typeIndexer.apply(type)).put(n).array();
    }

    @Override
    public String toString() {
        return (type == null) ? name : "{name: %s, type: %s}".formatted(name, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device device)) return false;
        return name.equals(device.name) && (type == null || device.type == null || Objects.equals(type, device.type));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
