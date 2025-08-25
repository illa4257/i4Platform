package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConvertIterator<E, R> implements Iterator<R> {
    private final Iterator<E> iter;
    private final Function<E, R> convertor;

    public ConvertIterator(final Iterator<E> iter, final Function<E, R> convertor) {
        this.iter = iter;
        this.convertor = convertor;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public R next() {
        return convertor.apply(iter.next());
    }

    @Override
    public void remove() {
        iter.remove();
    }

    @Override
    public void forEachRemaining(final Consumer<? super R> action) {
        iter.forEachRemaining(e -> action.accept(convertor.apply(e)));
    }
}