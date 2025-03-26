package illa4257.i4Utils.lists;

import java.util.Collection;
import java.util.Iterator;

/**
 * A thread-safe class that dynamically switches between two queues on each iterator call,
 * and removes elements during iteration.
 *
 * @param <T> the type of elements held in this queue
 */
public class SwappableTmpQueue<T> extends SwappableQueue<T> {
    public SwappableTmpQueue() {}
    public SwappableTmpQueue(final Collection<T> collection) { super(collection); }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<T> iter = SwappableTmpQueue.super.iterator();

            @Override public boolean hasNext() { return iter.hasNext(); }

            @Override
            public T next() {
                final T v = iter.next();
                iter.remove();
                return v;
            }
        };
    }
}