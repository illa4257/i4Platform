package illa4257.i4Utils.lists;

import java.util.Iterator;

public class ArrIterable<T> implements Iterable<T> {
    public final T[] array;

    public ArrIterable(final T[] array) { this.array = array; }

    @SuppressWarnings("NullableProblems") @Override public Iterator<T> iterator() { return new ArrIterator<>(array); }
}
