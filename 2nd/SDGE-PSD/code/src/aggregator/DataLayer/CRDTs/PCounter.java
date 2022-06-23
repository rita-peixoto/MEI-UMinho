package DataLayer.CRDTs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 *   PCounter is an increment only counter for distributed systems
 */

public class PCounter<I> {


    private final Map<I, Integer> counter;

    public PCounter() {
        this.counter = new HashMap<>();
    }

    public PCounter(I elem, Integer value) {
        this.counter = new HashMap<>();
        this.counter.put(elem, value);
    }

    public PCounter(Collection<I> c) {
        this.counter = new HashMap<>();
        c.forEach(x -> counter.put(x, 0));
    }

    public PCounter(PCounter<I> p) {
        this.counter = p.counter;
    }

    public PCounter(Map<I, Integer> map) {
        this.counter = new HashMap<>();
        map.entrySet().stream().filter(x -> x.getValue() > 0).forEach(e -> counter.put(e.getKey(), e.getValue()));
    }

    public static <I> PCounter<I> joinInto(PCounter<I> destination, PCounter<I> other) {
        destination.join(other);
        return destination;
    }

    public void increment(I id) {
        this.counter.put(id, this.counter.getOrDefault(id, 0) + 1);
    }

    public void add(I id, Integer value) {
        this.counter.put(id, this.counter.getOrDefault(id, 0) + value);
    }

    public Integer value() {
        return this.counter.values().stream().reduce(0, Integer::sum);
    }

    public Integer valueOf(I id) {
        return this.counter.getOrDefault(id, 0);
    }

    public void join(PCounter<I> c) {
        c.counter.forEach((k, v) -> {
            if (this.counter.containsKey(k)) this.counter.put(k, Integer.max(this.counter.get(k), v));
            else this.counter.put(k, v);
        });
    }

    @Override
    public String toString() {
        return "[(" + value() + "=" + counter + ")]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PCounter<?> pCounter)) return false;
        return counter.equals(pCounter.counter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(counter);
    }

    public Map<I, Integer> getCounters() {
        return new HashMap<>(this.counter);
    }
}