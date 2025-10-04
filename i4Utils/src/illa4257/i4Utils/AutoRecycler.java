package illa4257.i4Utils;

public class AutoRecycler<T> implements AutoCloseable {
    public final Recycler<T> recycler;
    public final T instance;

    public AutoRecycler(final Recycler<T> recycler) { this.recycler = recycler; instance = recycler.get(); }

    @Override
    public void close() throws Exception {
        recycler.recycle(instance);
    }
}