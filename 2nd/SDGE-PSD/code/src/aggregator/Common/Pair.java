package Common;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Pair<X, Y> implements Cloneable {

    private X first;
    private Y second;

    public Pair(X first, Y second) {
        this.first = first;
        this.second = second;
    }

    public Pair(Pair<X, Y> pair) {
        this.first = pair.first;
        this.second = pair.second;
    }

    public static <X, Y> Pair<X, Y> of(X first, Y second) {
        return new Pair<>(first, second);
    }

    public static <X, Y> Pair<X, Y> of(Map.Entry<X, Y> KVpair) {
        return new Pair<>(KVpair.getKey(), KVpair.getValue());
    }

    public static <X> X fst(Pair<X, ?> pair) {
        return pair.first;
    }

    public static <Y> Y snd(Pair<?, Y> pair) {
        return pair.second;
    }

    public X getFirst() {
        return first;
    }

    public void setFirst(X first) {
        this.first = first;
    }

    public void mapFirst(Function<X, X> function) {
        this.first = function.apply(this.first);
    }

    public Y getSecond() {
        return second;
    }

    public void setSecond(Y second) {
        this.second = second;
    }

    public void mapSecond(Function<Y, Y> function) {
        this.second = function.apply(this.second);
    }

    public <R> R apply(BiFunction<X, Y, R> function) {
        return function.apply(first, second);
    }

    public void map(Function<X, X> mapFST, Function<Y, Y> mapSND) {
        this.first = mapFST.apply(this.first);
        this.second = mapSND.apply(this.second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    protected Pair<X, Y> clone() {
        return new Pair<>(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ')';
    }
}