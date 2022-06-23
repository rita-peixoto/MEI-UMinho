package DataLayer.CRDTs;

import Common.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 *   PNCounter is an increment and decrement counter for distributed systems
 */

public class PNCounter<I> {


    private final Map<I, Pair<Integer, Integer>> counter;

    public PNCounter() {
        this.counter = new HashMap<>();
    }

    public PNCounter(I elem, Integer value) {
        this.counter = new HashMap<>();
        this.add(elem, value);
    }

    public PNCounter(Collection<I> c) {
        this.counter = new HashMap<>();
        c.forEach(x -> counter.put(x, new Pair<>(0, 0)));
    }

    public PNCounter(Map<I, Pair<Integer, Integer>> map) {
        this.counter = new HashMap<>();
        map.entrySet().stream().filter(x -> x.getValue().apply((a, b) -> a != 0 || b != 0)).forEach(x -> this.counter.put(x.getKey(), x.getValue()));
    }

    // increments 1 to the value in aggregatorId
    public void increment(I id) {
        try {
            this.counter.get(id).mapFirst(x -> x + 1);
        } catch (NullPointerException e) {
            this.counter.put(id, new Pair<>(1, 0));
        }
    }

    public void decrement(I id) {
        try {
            this.counter.get(id).mapSecond(x -> x + 1);
        } catch (NullPointerException e) {
            this.counter.put(id, new Pair<>(0, 1));
        }
    }

    public void removeId(I id) {
        var p = this.counter.get(id);
        if (p != null) {
            int val = p.apply(Integer::max);
            p.map(x -> val, x -> val);
        }
    }

    public void add(I id, Integer value) {
        if (value > 0) {
            try {
                this.counter.get(id).mapFirst(x -> x + value);
            } catch (NullPointerException e) {
                this.counter.put(id, new Pair<>(value, 0));
            }
        } else {
            try {
                this.counter.get(id).mapSecond(x -> x + value);
            } catch (NullPointerException e) {
                this.counter.put(id, new Pair<>(0, value));
            }
        }
    }

    public Integer value() {
        return this.counter.values().stream().map(p -> p.apply((x, y) -> x - y)).reduce(0, Integer::sum);
    }

    public Integer valueOf(I id) {
        try {
            return this.counter.get(id).apply((a, b) -> a - b);
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public void join(PNCounter<I> c) {
        c.counter.forEach((k, v) -> {
            Pair<Integer, Integer> current = this.counter.get(k);
            if (current == null) {
                this.counter.put(k, new Pair<>(v));
            } else {
                current.map(x -> Integer.max(x, v.getFirst()), y -> Integer.max(y, v.getSecond()));
            }
        });
    }

    @Override
    public String toString() {
        return "[(" + value() + "=" + counter + ")]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PNCounter<?> pnCounter)) return false;
        return counter.equals(pnCounter.counter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counter);
    }

    public Map<I, Pair<Integer, Integer>> getCounters() {
        return new HashMap<>(this.counter);
    }

    public Pair<Integer, Integer> getPair(I id) {
        Pair<Integer, Integer> p = this.counter.get(id);
        return p == null ? null : new Pair<>(p);
    }

}