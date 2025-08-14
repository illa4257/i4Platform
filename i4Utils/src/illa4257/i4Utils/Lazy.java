package illa4257.i4Utils;

import java.util.function.Supplier;

public class Lazy<T> {
    private final Supplier<T> supplier;
    private volatile T value = null;

    public Lazy(final Supplier<T> supplier) { this.supplier = supplier; }

    public boolean isInitialized() { return value != null; }
    public void set(final T value) { this.value = value; }
    public void remove() { value = null; }

    public T get() {
        T v = value;
        if (v == null)
            synchronized (this) {
                if ((v = value) == null)
                    value = v = supplier.get();
            }
        return v;
    }
}