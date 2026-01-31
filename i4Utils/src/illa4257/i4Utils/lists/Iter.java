package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class Iter {
    public static <T> Iterator<T> skip(final Iterator<T> iterator, int n) {
        for (; n > 0; n--)
            iterator.next();
        return iterator;
    }

    public static <T> Iterable<T> reversible(final List<T> list) {
        return () -> reverse(list);
    }

    public static <T> Iterator<T> reverse(final List<T> list) {
        return new Iterator<T>() {
            public final ListIterator<T> listIterator = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return listIterator.hasPrevious();
            }

            @Override
            public T next() {
                return listIterator.previous();
            }

            @Override
            public void remove() {
                listIterator.remove();
            }
        };
    }
}