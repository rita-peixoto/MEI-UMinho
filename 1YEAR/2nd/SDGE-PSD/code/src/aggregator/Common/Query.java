package Common;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Query represents a request made by a client
 *   such request is made of a search type (zonal or global),
 *   an operation, and possibly an argument.
 */
public class Query {
    private final SearchType searchType;
    private final QueryOp queryOp;
    private final String argument;

    // used by client to create a new query
    public Query(SearchType searchType, QueryOp queryOp, String argument) {
        this.searchType = searchType;
        this.queryOp = queryOp;
        this.argument = argument == null ? "" : argument;
    }

    /**
     * used by aggregator to create Query through the message received
     */
    public Query(byte[] byteArrayQuery) {
        this.searchType = SearchType.fromValue(byteArrayQuery[0]);
        this.queryOp = QueryOp.fromValue(byteArrayQuery[1]);
        this.argument = new String(byteArrayQuery, 2, byteArrayQuery.length - 2);
    }

    public static Query of(byte[] byteArrayQuery) {
        return new Query(byteArrayQuery);
    }

    /**
     * used by client to convert Query to message to be sent to aggregator
     *
     * @return array of bytes with format: | 1st byte = searchType | 2nd byte = query operation | rest is a string with arguments |
     **/
    public byte[] toByteArray() {
        byte[] argumentBytes = this.argument.getBytes();
        byte[] queryByte = new byte[2 + argumentBytes.length];
        queryByte[0] = this.searchType.value;
        queryByte[1] = this.queryOp.value;
        System.arraycopy(argumentBytes, 0, queryByte, 2, argumentBytes.length);
        return queryByte;
    }

    public String getArgument() {
        return this.argument;
    }

    public QueryOp getQueryOp() {
        return this.queryOp;
    }

    public SearchType getSearchType() {
        return this.searchType;
    }

    public boolean is(QueryOp op) {
        return this.queryOp.equals(op);
    }

    public boolean is(SearchType type) {
        return this.searchType.equals(type);
    }


    // search type can be either zonal for query in client/aggregator zone or global for query in all system
    public enum SearchType {
        ZONAL((byte) 1),
        GLOBAL((byte) 2);

        // map byte to SearchType
        private static final Map<Byte, SearchType> fromValMap = Arrays.stream(SearchType.values()).collect(Collectors.toMap(x -> x.value, x -> x));
        // byte value to put in message sent
        public final byte value;

        private SearchType(byte value) {
            this.value = value;
        }

        private static SearchType fromValue(byte val) {
            return fromValMap.get(val);
        }
    }

    /**
     * Possible operations that client can make are:
     * - ONLINE: search if a device with id = argument is online
     * - DEVICES: search number of devices of type online (if no argument given all types, else type = argument)
     * - ACTIVE: search the number of active devices
     * - EVENT: search number of events ocurred of type = argument
     */
    public enum QueryOp {
        ONLINE((byte) 1),
        DEVICES((byte) 2),
        ACTIVE((byte) 3),
        EVENT((byte) 4);

        private static final Map<Byte, QueryOp> fromValMap = Arrays.stream(QueryOp.values()).collect(Collectors.toMap(x -> x.value, x -> x));
        public final byte value;

        private QueryOp(byte value) {
            this.value = value;
        }

        private static QueryOp fromValue(byte val) {
            return fromValMap.get(val);
        }
    }
}
