package Common;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum AGTag {

    SUBSCRIBE,
    FWD_SUBSCRIPTION,
    UNSUBSCRIBE,
    CONNECTION,
    CONNECTED,
    GOSSIP;

    private static final Map<Integer, AGTag> fromOrdMap = Arrays.stream(AGTag.values()).collect(Collectors.toMap(Enum::ordinal, x -> x));

    private static AGTag fromOrdinal(int ordinal) {
        return fromOrdMap.get(ordinal);
    }

    public static AGTag fromBytes(byte[] b) {
        return fromOrdinal(ByteBuffer.wrap(b).getInt());
    }

    public byte[] getBytes() {
        return ByteBuffer.allocate(Integer.SIZE / 4).putInt(this.ordinal()).array();
    }

}
