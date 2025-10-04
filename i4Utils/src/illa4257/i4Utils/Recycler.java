package illa4257.i4Utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Recycler<T> {
    /// Use it if you need cleanup something.
    public final ConcurrentLinkedQueue<T> instances = new ConcurrentLinkedQueue<>();
    private final Supplier<T> supplier;
    private final Consumer<T> recycler;

    public Recycler(final Supplier<T> supplier, final Consumer<T> recycler) {
        this.supplier = supplier;
        this.recycler = recycler;
    }

    public T get() {
        final T r = instances.poll();
        return r != null ? r : supplier.get();
    }

    public void recycle(final T object) {
        if (recycler != null)
            recycler.accept(object);
        instances.offer(object);
    }
}