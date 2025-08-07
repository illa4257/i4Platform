package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConvertIterator<E> implements Iterator<E> {
    private final Iterator<E> iter;
    private final Function<E, E> convertor;

    public ConvertIterator(final Iterator<E> iter, final Function<E, E> convertor) {
        this.iter = iter;
        this.convertor = convertor;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public E next() {
        return convertor.apply(iter.next());
    }

    @Override
    public void remove() {
        iter.remove();
    }

    @Override
    public void forEachRemaining(final Consumer<? super E> action) {
        iter.forEachRemaining(e -> action.accept(convertor.apply(e)));
    }
}