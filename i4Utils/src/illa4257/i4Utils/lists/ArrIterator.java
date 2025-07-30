package illa4257.i4Utils.lists;

import java.util.Iterator;

public class ArrIterator<T> implements Iterator<T> {
    public int index = 0;
    public final T[] array;

    public ArrIterator(final T[] array) { this.array = array; }
    public ArrIterator(final int startIndex, final T[] array) { this.index = startIndex; this.array = array; }

    @Override
    public boolean hasNext() {
        return array.length > index;
    }

    @Override
    public T next() {
        return array[index++];
    }
}
