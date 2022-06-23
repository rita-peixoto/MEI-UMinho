package DataLayer.CRDTs;

import Common.StaticUtilities;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static Common.StaticUtilities.*;

public class ORSet<E, I> {
    private final Map<E, Map<I, Integer>> orSet;
    private final Map<I, Integer> counters;

    public ORSet() {
        this.orSet = new HashMap<>();
        this.counters = new HashMap<>();
    }

    public ORSet(Map<E, Map<I, Integer>> orSet, Map<I, Integer> counters) {
        this.orSet = new HashMap<>();
        orSet.entrySet().stream().filter(x -> !x.getValue().isEmpty()).forEach(x -> this.orSet.put(x.getKey(), new HashMap<>(x.getValue())));
        this.counters = new HashMap<>(counters);
    }

    public Set<E> elements() {
        return orSet.keySet();
    }

    public Set<E> elementsOf(I id) {
        return orSet.entrySet().stream().filter(x -> x.getValue().containsKey(id)).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Set<I> ids() {
        return orSet.values().stream().map(Map::keySet).reduce(new HashSet<>(), StaticUtilities::setUnion);
    }

    public void registerId(I id) {
        counters.putIfAbsent(id, 0);
    }

    public int size() {
        return this.orSet.size();
    }

    public Long sizeOf(I id) {
        return this.orSet.values().stream().filter(x -> x.containsKey(id)).count();
    }

    public boolean contains(E elem) {
        return orSet.containsKey(elem);
    }

    public boolean contains(E elem, I id) {
        var map = orSet.getOrDefault(elem, null);
        return map == null ? false : map.containsKey(id);
    }

    public boolean containsId(I i) {
        return counters.containsKey(i);
    }

    private int increment(I id) {
        try {
            int c = counters.get(id) + 1;
            counters.put(id, c);
            return c;
        } catch (NullPointerException e) {
            counters.put(id, 1);
            return 1;
        }
    }

    public boolean add(E element, I id) {
        int c = increment(id);
        try {
            orSet.get(element).put(id, c);
            return false;
        } catch (NullPointerException e) {
            orSet.put(element, new HashMap<>(Map.of(id, c)));
            return true;
        }
    }


    public boolean remove(E element) {
        return orSet.remove(element) != null;
    }

    public boolean removeOn(E element, I id) {
        try {
            boolean res = orSet.get(element).remove(id) != null;
            if (orSet.get(element).isEmpty())
                orSet.remove(element);
            return res;
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Base Expression:
     * (r,rc) ⊔  (s,sc) = (r ∩ s) ∪ ( r \ sc) ∪ ( s \ rc)
     * <p>
     * The implementation follows a slight tradeoff for better performance in most cases.
     * Instead of performing the complements with the entire counter,
     * each set of changes is precalculated to the elements that it can have and effect on. <p>
     * (outdatedInS = [ x -> s(x) | \forall x \in (rc \ sc)]) <p>
     * (outdatedInR = [ x -> r(x) | \forall x \in (sc \ rc )])<p>
     * As a result, the complements (r \ sc) and (s \ rc) are replaced with [x -> r(x) | x \in r /\ r(x) > outdatedInS(x)] and [x -> s(x) | x \in s /\ s(x) > outdatedInR(x)]
     * <p>
     * This results in a boost in performance for join that only update a few of the keys, which will be most of the cases
     *
     * @param r           The first set
     * @param outdatedInS The outdated keys in s
     * @param s           The second set
     * @param outdatedInR The outdated keys in r
     * @return The resulting set of both unions
     */
    private Map<I, Integer> DotSetUnion(Map<I, Integer> r, Map<I, Integer> s, Map<I, Integer> outdatedInS, Map<I, Integer> outdatedInR) {
        final Map<I, Integer> result =
                setIntersection(new HashSet<>(r.entrySet()), s.entrySet())
                        .stream().collect(KVStreamToMap());

        outdatedInS.forEach((k, v) -> {
            Integer val = r.get(k);
            if (val != null && val > v)
                result.put(k, val);
        });

        outdatedInR.forEach((k, v) -> {
            Integer val = s.get(k);
            if (val != null && val > v)
                result.put(k, val);
        });

        return result;
    }

    public boolean join(ORSet<E, I> other) {

        Map<I, Integer> externallyOutdated = setComplement(new HashSet<>(this.counters.entrySet()), other.counters.entrySet())
                .stream().map(Entry::getKey).collect(Collectors.toMap(x -> x, x -> other.counters.getOrDefault(x, 0)));

        Map<I, Integer> internallyOutdated = setComplement(new HashSet<>(other.counters.entrySet()), this.counters.entrySet())
                .stream().map(Entry::getKey).collect(Collectors.toMap(x -> x, x -> this.counters.getOrDefault(x, 0)));

        Set<E> elements = setUnion(new HashSet<>(this.orSet.keySet()), other.orSet.keySet());

        boolean result = elements.stream().map(e -> {
            Map<I, Integer> r = this.orSet.getOrDefault(e, Map.of());
            Map<I, Integer> s = other.orSet.getOrDefault(e, Map.of());

            Map<I, Integer> p = DotSetUnion(r, s, externallyOutdated, internallyOutdated);

            if (!p.isEmpty())
                return this.orSet.put(e, p) == null; // null means the element is new
            else
                return this.orSet.remove(e) != null; // not null means the element was removed

        }).reduce(false, Boolean::logicalOr);

        // Merging the counters - Must occur after the DotDet merges because java encapsulations sucks :')
        other.counters.forEach((k, v) -> this.counters.merge(k, v, Integer::max));

        return result;
    }

    public Map<E, Map<I, Integer>> getOrSet() {
        return orSet.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> new HashMap<>(x.getValue())));
    }

    public boolean removeId(I id) {
        boolean res = this.orSet.values().stream().map(x -> x.remove(id) != null).reduce(false, Boolean::logicalOr);
        this.orSet.entrySet().stream().filter(x -> x.getValue().isEmpty()).map(Entry::getKey).toList().forEach(orSet::remove);
        return res;
    }

    public Map<I, Integer> getCounters() {
        return new HashMap<>(counters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ORSet<?, ?> orSet1)) return false;
        return orSet.equals(orSet1.orSet) && counters.equals(orSet1.counters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orSet, counters);
    }

    @Override
    public String toString() {
        return "[[ %s | %s ]]".formatted(counters, orSet);
    }

}
