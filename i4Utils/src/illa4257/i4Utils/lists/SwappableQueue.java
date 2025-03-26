package illa4257.i4Utils.lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A thread-safe class that dynamically switches between two queues on each iterator call.
 *
 * @param <T> the type of elements held in this queue
 */
public class SwappableQueue<T> implements Iterable<T> {
    private final Object locker = new Object();
    private volatile boolean value = true;
    private final ConcurrentLinkedQueue<T> queue1, queue2 = new ConcurrentLinkedQueue<>();

    public SwappableQueue() { queue1 = new ConcurrentLinkedQueue<>(); }
    public SwappableQueue(final Collection<T> collection) { queue1 = new ConcurrentLinkedQueue<>(collection); }

    public boolean isEmpty() { return queue1.isEmpty() && queue2.isEmpty(); }
    public int size() { return queue1.size() + queue2.size(); }

    public boolean add(final T e) { return (value ? queue1 : queue2).add(e); }

    public ConcurrentLinkedQueue<T> get() { return value ? queue2 : queue1; }
    public ConcurrentLinkedQueue<T> swap() {
        synchronized (locker) {
            return (value = !value) ? queue2 : queue1;
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        synchronized (locker) {
            return ((value = !value) ? queue2 : queue1).iterator();
        }
    }
}