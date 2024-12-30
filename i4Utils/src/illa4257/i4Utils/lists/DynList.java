package illa4257.i4Utils.lists;

import java.lang.reflect.Array;

public class DynList<T> {
    private final Object locker = new Object();
    private final Class<T> c;
    private Page iter;
    private final Page root;
    private Page current;

    public DynList(final Class<T> c) {
        this(c, 8);
    }

    public DynList(final Class<T> c, final int pageLimit) {
        if (pageLimit <= 0)
            throw new IllegalArgumentException("Page limit cannot be negative or zero");
        this.c = c;
        iter = root = current = new Page(pageLimit);
    }

    private class Page {
        private final Object pageLocker = new Object();
        private final T[] array;
        private int i = 0, sz = 0;
        private Page next = null;

        @SuppressWarnings("unchecked")
        public Page(final int size) { array = (T[]) Array.newInstance(c, size); }

        public void reset() {
            i = 0;
            synchronized (pageLocker) {
                if (next != null)
                    next.reset();
            }
        }

        public void add(final T value) {
            synchronized (pageLocker) {
                array[sz++] = value;
                if (sz == array.length)
                    current = next = new Page(array.length);
            }
        }

        public T next() {
            synchronized (pageLocker) {
                if (i == sz) {
                    iter = next;
                    return next != null ? next.next() : null;
                }
                return array[i++];
            }
        }
    }

    public void add(final T value) {
        if (value == null)
            return;
        synchronized (locker) {
            current.add(value);
        }
    }

    public void remove(final T value) {
        if (value == null)
            return;
        int i = 0;
        Page c;
        synchronized (locker) {
            c = root;
            synchronized (c.pageLocker) {
                if (c.sz > 0 && c.array[0] == value) {
                    for (i++; i < c.sz; i++)
                        c.array[i - 1] = c.array[i];
                    c.array[i] = null;
                    c.sz--;
                    return;
                }
            }
        }
        while (c != null)
            synchronized (c.pageLocker) {
                if (c.sz == 0) {
                    c = c.next;
                    continue;
                }
                for (i = 0; i < c.sz; i++)
                    if (c.array[i] == value) {
                        for (i++; i < c.sz; i++)
                            c.array[i - 1] = c.array[i];
                        c.array[i] = null;
                        c.sz--;
                        return;
                    }
                c = c.next;
            }
    }

    public void reset() {
        synchronized (locker) {
            iter = root;
            iter.reset();
        }
    }

    public T next() {
        synchronized (locker) {
            if (iter == null)
                return null;
            return iter.next();
        }
    }
}