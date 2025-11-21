package illa4257.i4Utils.lists;

public interface Grid<E> {
    int width();
    int height();

    E set(final int x, final int y, final E element);
    E get(final int x, final int y);
    void clear();
}