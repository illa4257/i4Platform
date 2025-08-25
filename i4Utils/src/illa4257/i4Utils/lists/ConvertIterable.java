package illa4257.i4Utils.lists;

import java.util.Iterator;
import java.util.function.Function;

public class ConvertIterable<E, R> implements Iterable<R> {
    private final Iterable<E> iter;
    private final Function<E, R> convertor;

    public ConvertIterable(final Iterable<E> iter, final Function<E, R> convertor) {
        this.iter = iter;
        this.convertor = convertor;
    }
    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<R> iterator() {
        return new ConvertIterator<>(iter.iterator(), convertor);
    }
}