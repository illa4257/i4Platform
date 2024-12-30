package illa4257.i4Utils.lists;

import java.lang.reflect.Array;
import java.util.Iterator;

public class PagedTmpList<T> implements Iterable<T> {
    private final Object locker = new Object();
    private final Class<T> c;
    private Page iter = null, page, current;

    public PagedTmpList(final Class<T> c) {
        this(c, 8);
    }

    public PagedTmpList(final Class<T> c, final int pageLimit) {
        if (pageLimit <= 0)
            throw new IllegalArgumentException("Page limit cannot be negative or zero");
        this.c = c;
        page = current = new Page(pageLimit);
    }

    private class Page {
        private final T[] array;
        private int i = 0;
        private Page next = null;

        @SuppressWarnings("unchecked")
        public Page(final int size) { array = (T[]) Array.newInstance(c, size); }

        public void reset() {
            i = 0;
            if (next != null)
                next.reset();
        }

        public void add(final T value) {
            array[i++] = value;
            if (i == array.length)
                current = next = new Page(array.length);
        }
    }

    public void add(final T value) {
        if (value == null)
            return;
        synchronized (locker) {
            current.add(value);
        }
    }

    public void nextPage() {
        synchronized (locker) {
            if ((iter != null && iter.array[iter.i] != null) || page.i == 0)
                return;
            iter = page;
            iter.reset();
            page = current = new Page(page.array.length);
        }
    }

    public boolean hasNext() {
        synchronized (locker) {
            return iter != null && iter.i < iter.array.length && iter.array[iter.i] != null;
        }
    }

    public T next() {
        synchronized (locker) {
            if (iter == null)
                return null;
            if (iter.i == iter.array.length) {
                iter = iter.next;
                return next();
            }
            final T v = iter.array[iter.i++];
            if (v == null) {
                iter = iter.next;
                return next();
            }
            return v;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return PagedTmpList.this.hasNext();
            }

            @Override
            public T next() {
                return PagedTmpList.this.next();
            }
        };
    }

    @Override
    public String toString() {
        Iterator<T> iter = iterator();
        if (!iter.hasNext()) {
            return "[]";
        } else {
            final StringBuilder b = new StringBuilder();
            b.append('[');

            while(true) {
                final T value = iter.next();
                b.append(value == this ? "(this Collection)" : value);

                if (!iter.hasNext())
                    return b.append(']').toString();

                b.append(',').append(' ');
            }
        }
    }
}