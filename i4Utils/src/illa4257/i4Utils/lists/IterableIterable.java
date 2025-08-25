package illa4257.i4Utils.lists;

import java.util.Iterator;

public class IterableIterable<T> implements Iterable<T> {
    private final Iterable<Iterable<T>> iterables;

    public IterableIterable(final Iterable<Iterable<T>> iterables) { this.iterables = iterables; }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return new IteratorIterable<>(iterables.iterator());
    }
}