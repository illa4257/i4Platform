package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.function.Consumer;

public class IteratorIterable<T> implements Iterator<T> {
    private final Iterator<? extends Iterable<T>> iters;
    private Iterator<T> iter = null;

    public IteratorIterable(final Iterator<? extends Iterable<T>> iterables) { iters = iterables; }

    @Override
    public boolean hasNext() {
        if (iter != null && iter.hasNext())
            return true;
        while (iters.hasNext())
            if ((iter = iters.next().iterator()).hasNext())
                return true;
        return false;
    }

    @Override
    public T next() {
        if (iter != null && iter.hasNext())
            return iter.next();
        while (true)
            if ((iter = iters.next().iterator()).hasNext())
                return iter.next();
    }

    @Override public void remove() { iter.remove(); }

    @Override
    public void forEachRemaining(final Consumer<? super T> action) {
        if (iter != null)
            iter.forEachRemaining(action);
        while (iters.hasNext())
            iters.next().iterator().forEachRemaining(action);
    }
}
