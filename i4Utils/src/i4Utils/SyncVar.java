package i4Utils;

public class SyncVar<T> {
    public final Object locker = new Object();

    private T v;

    public SyncVar() { v = null; }
    public SyncVar(final T value) { v = value; }

    public void set(final T value) { synchronized (locker) { v = value; } }

    public T getAndSet(final T value) {
        synchronized (locker) {
            final T old = v;
            v = value;
            return old;
        }
    }

    public T get() { synchronized (locker) { return v; } }

    @Override public String toString() { synchronized (locker) { return v == null ? null : v.toString(); } }
}