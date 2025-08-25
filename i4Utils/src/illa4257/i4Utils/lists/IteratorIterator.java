package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.function.Consumer;

public class IteratorIterator<T> implements Iterator<T> {
    private final Iterator<Iterator<T>> iterators;
    private Iterator<T> current = null;

    public IteratorIterator(final Iterator<Iterator<T>> iterators) { this.iterators = iterators; }

    @Override
    public boolean hasNext() {
        if (current != null && current.hasNext())
            return true;
        while (iterators.hasNext())
            if ((current = iterators.next()).hasNext())
                return true;
        return false;
    }

    @Override
    public T next() {
        if (current != null && current.hasNext())
            return current.next();
        while (true) {
            current = iterators.next();
            if (current.hasNext())
                return current.next();
        }
    }

    @Override
    public void remove() {
        current.remove();
    }

    @Override
    public void forEachRemaining(final Consumer<? super T> action) {
        if (current != null)
            current.forEachRemaining(action);
        while (iterators.hasNext())
            iterators.next().forEachRemaining(action);
    }
}