package Common;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Contains Procedures used all over the implementation
 */
public interface StaticUtilities {
    /**
     * Generates an even random selection of multiple elements from the given collection
     *
     * @param count      The number of elements to select
     * @param collection The collection to sample
     * @param <X>        The type of the elements of the collection
     * @return The sampled elements paired with the number of times choosen
     */
    static <X> Map<X, Integer> sampleMultipleEvenly(int count, Collection<X> collection) {
        if (count < 1 || collection.isEmpty())
            return new HashMap<>();

        final int base = count / collection.size();
        final int extra = count % collection.size();

        List<X> random = new ArrayList<>(collection);
        Collections.shuffle(random);
        List<X> res = random.subList(0, extra);

        if (base == 0)
            return res.stream().collect(Collectors.toMap(x -> x, x -> 1));
        else
            return collection.stream().collect(Collectors.toMap(x -> x, x -> res.contains(x) ? base + 1 : base));
    }

    // The " I'm turning Java into a weird lambda calculus language" Section:
    static void lockWrapping(Runnable task, Lock lock, Lock... more) {
        lock.lock();
        Arrays.stream(more).sequential().forEach(Lock::lock);
        try {
            task.run();
        } finally {
            lock.unlock();
            Arrays.stream(more).forEach(Lock::unlock);
        }
    }

    static <V> V lockWrapping(Callable<V> task, Lock lock, Lock... more) {
        lock.lock();
        Arrays.stream(more).sequential().forEach(Lock::lock);
        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
            Arrays.stream(more).forEach(Lock::unlock);
        }
    }

    static void readLockWrapping(Runnable task, ReadWriteLock lock, ReadWriteLock... more) {
        lock.readLock().lock();
        Arrays.stream(more).sequential().forEach(l -> l.readLock().lock());
        try {
            task.run();
        } finally {
            lock.readLock().unlock();
            Arrays.stream(more).forEach(l -> l.readLock().unlock());
        }
    }

    static <V> V readLockWrapping(Callable<V> task, ReadWriteLock lock, ReadWriteLock... more) {
        lock.readLock().lock();
        Arrays.stream(more).sequential().forEach(l -> l.readLock().lock());
        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.readLock().unlock();
            Arrays.stream(more).forEach(l -> l.readLock().unlock());
        }
    }

    static void writeLockWrapping(Runnable task, ReadWriteLock lock, ReadWriteLock... more) {
        lock.writeLock().lock();
        Arrays.stream(more).sequential().forEach(l -> l.writeLock().lock());
        try {
            task.run();
        } finally {
            lock.writeLock().unlock();
            Arrays.stream(more).forEach(l -> l.writeLock().unlock());
        }
    }

    static <V> V writeLockWrapping(Callable<V> task, ReadWriteLock lock, ReadWriteLock... more) {
        lock.writeLock().lock();
        Arrays.stream(more).sequential().forEach(l -> l.writeLock().lock());
        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.writeLock().unlock();
            Arrays.stream(more).forEach(l -> l.writeLock().unlock());
        }
    }

    static <V> V catchReturn(Callable<V> callable, V orElse) {
        try {
            return callable.call();
        } catch (Exception e) {
            return orElse;
        }
    }

    static void runtimeException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void guard(Boolean b, Runnable tt, Runnable ff) {
        try {
            if (b)
                tt.run();
            else
                ff.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <V> V runtimeException(Callable<V> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T> T guard(Boolean b, Callable<T> tt, Callable<T> ff) {
        try {
            if (b)
                return tt.call();
            else
                return ff.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void guard(Boolean b, Runnable tt) {
        try {
            if (b)
                tt.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static <T, R> R guard(Boolean b, T t, Function<T, R> tt, Function<T, R> ff) {
        try {
            if (b)
                return tt.apply(t);
            else
                return ff.apply(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // Serialization Utils

    static byte[] intToBytes(int i) {
        return ByteBuffer.allocate(Integer.SIZE / 4).putInt(i).array();
    }

    static int bytesToInt(byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    static <X> byte[] serializeList(List<X> array, Function<X, byte[]> serializer) {
        if (array.size() == 0) return null;
        List<byte[]> sers = array.stream().map(serializer).toList();
        ByteBuffer res = ByteBuffer.allocate(sers.stream().map(x -> x.length).reduce(array.size() * 4, Integer::sum));
        sers.forEach(x -> {
            res.putInt(x.length);
            res.put(x);
        });
        return res.array();
    }

    static <X> List<X> deserializeList(byte[] frame, Function<byte[], X> deserializer) {
        final List<X> result = new ArrayList<>();
        if (frame == null) return result;
        ByteBuffer buffer = ByteBuffer.wrap(frame);
        while (buffer.hasRemaining()) {
            int ssize = buffer.getInt();
            byte[] sbuff = new byte[ssize];
            buffer.get(sbuff);
            result.add(deserializer.apply(sbuff));
        }
        return result;
    }

    static <T> Set<T> setUnion(Set<T> sub, Set<T> s) {
        sub.addAll(s);
        return sub;
    }

    static <T> Set<T> setIntersection(Set<T> x, Set<T> y) {
        x.retainAll(y);
        return x;
    }

    static <T> Set<T> setComplement(Set<T> x, Set<T> y) {
        x.removeAll(y);
        return x;
    }

    static <A, B> Collector<Map.Entry<A, B>, ?, Map<A, B>> KVStreamToMap() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }
}


