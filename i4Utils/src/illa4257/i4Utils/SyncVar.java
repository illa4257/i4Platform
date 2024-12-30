package illa4257.i4Utils;

import java.util.Objects;

public class SyncVar<T> {
    public final Object locker = new Object();

    private T v;

    public SyncVar() { v = null; }
    public SyncVar(final T value) { v = value; }

    public void set(final T value) { synchronized (locker) { v = value; } }

    public void setIfNull(final T newValue) {
        if (newValue == null)
            return;
        synchronized (locker) {
            if (v == null)
                v = newValue;
        }
    }

    public boolean setIfNotEquals(final T newValue) {
        synchronized (locker) {
            if (Objects.equals(v, newValue))
                return false;
            v = newValue;
            return true;
        }
    }

    public T getAndSet(final T value) {
        synchronized (locker) {
            final T old = v;
            v = value;
            return old;
        }
    }

    public T get() { synchronized (locker) { return v; } }

    /**
     * @param alt Alternative object
     * @return Returns value. If the value is null, it will return alt
     */
    public T get(final T alt) {
        synchronized (locker) {
            return v == null ? alt : v;
        }
    }

    @Override public String toString() { synchronized (locker) { return v == null ? null : v.toString(); } }
}