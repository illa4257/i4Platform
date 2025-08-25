package illa4257.i4Utils.lists;

import java.util.Iterator;

public class ArrIterable<T> implements Iterable<T> {
    public final T[] array;
    private final int index;

    public ArrIterable(final T[] array) { index = 0; this.array = array; }
    public ArrIterable(final int index, final T[] array) { this.index = index; this.array = array; }

    @SuppressWarnings("NullableProblems") @Override public Iterator<T> iterator() { return new ArrIterator<>(index, array); }
}
