package illa4257.i4Utils.lists;

public class ArrStack<T> {
    private int i = 0;
    private Object[] elems;

    public ArrStack() { this(8); }
    public ArrStack(final int initialCapacity) { elems = new Object[initialCapacity]; }

    public void push(final T elem) {
        if (i >= elems.length) {
            final Object[] na = new Object[elems.length << 1];
            System.arraycopy(elems, 0, na, 0, elems.length);
            elems = na;
        }
        elems[i++] = elem;
    }

    public T pop() {
        if (i == 0)
            return null;
        //noinspection unchecked
        final T r = (T) elems[--i];
        elems[i] = null;
        return r;
    }
}