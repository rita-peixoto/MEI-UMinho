import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileIPSet implements Set<String> {

    private final String filename;
    private final int step = 4;
    private String self = null;
    private int port = 8000;

    public FileIPSet(String filename) {
        this.filename = filename;
    }

    public static <V> V catchIOReturn(Callable<V> callable, V orElse) {
        try {
            return callable.call();
        } catch (IOException e) {
            return orElse;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getAddress() {
        return self;
    }

    /**
     * returns port number
     *
     * @return port
     */
    public int getPort() {
        return this.port;
    }

    public void registerRandom() throws IOException {
        unregister();
        this.getSet().stream().map(s -> Integer.parseInt(s.split(":")[1])).reduce(Integer::max).ifPresent(p -> port = p + step);
        self = "*:" + port;
        this.add(self);
    }

    public void registerTo(String id) throws IOException {
        unregister();
        if (getSet().contains(id)) throw new IOException("Already Registered Address");
        port = Integer.parseInt(id.split(":")[1]);
        self = id;
        this.add(self);
    }

    public void unregister() {
        if (self != null)
            this.remove(self);
    }

    private Set<String> getSet() throws IOException {
        try (Stream<String> stream = Files.lines(Path.of(filename))) {
            return stream.filter(s -> !s.isBlank()).map(String::strip).collect(Collectors.toSet());
        } catch (NoSuchFileException e) {
            return new HashSet<>();
        }
    }

    private Set<String> getSetWithoutSelf() throws IOException {
        Set<String> res = getSet();
        res.remove(self);
        return res;
    }

    private void rewriteFile(Set<String> set) {
        try {
            if (!set.isEmpty()) {
                PrintWriter writer = new PrintWriter(filename);
                set.forEach(writer::println);
                writer.close();
            } else {
                File f = new File(filename);
                if (f.isFile())
                    f.delete();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> boolean rewriteIfOk(Set<String> set, T object, BiPredicate<Set<String>, T> action) {
        if (action.test(set, object)) {
            rewriteFile(set);
            return true;
        }
        return false;
    }


    @Override
    public int size() {
        return catchIOReturn(() -> getSetWithoutSelf().size(), 0);
    }

    @Override
    public boolean isEmpty() {
        return catchIOReturn(() -> getSetWithoutSelf().isEmpty(), true);
    }

    @Override
    public boolean contains(Object o) {
        return catchIOReturn(() -> getSetWithoutSelf().contains(o), false);
    }

    @Override
    public Iterator<String> iterator() {
        return catchIOReturn(() -> getSetWithoutSelf().iterator(), Collections.emptyIterator());
    }

    @Override
    public Object[] toArray() {
        return catchIOReturn(() -> getSetWithoutSelf().toArray(), new String[0]);
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return catchIOReturn(() -> getSetWithoutSelf().toArray(a), a);
    }

    @Override
    public boolean add(String s) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), s, Set::add), false);
    }

    @Override
    public boolean remove(Object o) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), o, Set::remove), false);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), c, Set::containsAll), false);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), c, Set::addAll), false);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), c, Set::retainAll), false);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return catchIOReturn(() -> rewriteIfOk(getSet(), c, Set::removeAll), false);
    }

    @Override
    public void clear() {
        rewriteFile(self == null ? Set.of() : Set.of(self));
    }
}