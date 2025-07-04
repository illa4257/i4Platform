package illa4257.i4Utils.lists;

import java.util.Arrays;
import java.util.Iterator;

public class ArrNotifier<T> {
    private final Object locker = new Object();
    private int index = 0;
    private final T[] arr;

    public ArrNotifier(final T[] buff) { this.arr = buff; }

    public void add(final T element) {
        synchronized (locker) {
            arr[index++ % arr.length] = element;
            locker.notifyAll();
        }
    }

    public void clear() {
        synchronized (locker) {
            index = 0;
            Arrays.fill(arr, null);
        }
    }

    public class Monitor implements Iterator<T> {
        private int i = 0;

        public boolean isAvailable() {
            synchronized (locker) {
                return i != index;
            }
        }

        @Override public boolean hasNext() { return true; }

        public T next() {
            try {
                synchronized (locker) {
                    if (i == index)
                        locker.wait();
                    if (index - i > arr.length)
                        i = index - arr.length;
                    return arr[i++ % arr.length];
                }
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public Monitor newMonitor() { return new Monitor(); }

    @Override
    public String toString() { return Arrays.toString(arr); }
}