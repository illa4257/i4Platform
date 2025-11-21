package illa4257.i4Utils.lists;

import java.util.Arrays;

public class ArrayGrid<E> implements Grid<E> {
    private static final Object[] EMPTY_ELEMENT_DATA = new Object[0];

    private Object[] elementData;
    private int width, height;

    public ArrayGrid() {
        width = height = 0;
        elementData = EMPTY_ELEMENT_DATA;
    }

    public ArrayGrid(final int width, final int height) {
        if (width > 0 && height > 0)
            this.elementData = new Object[width * height];
        else if (width < 0 || height < 0)
            throw new IllegalArgumentException("Illegal Size: " + width + "x" + height);
        else
            this.elementData = EMPTY_ELEMENT_DATA;
        this.width = width;
        this.height = height;
    }

    public void ensureCapacity(final int width, final int height) {
        if (width > this.width || height > this.height) {
            final int ow = this.width, oh = this.height;
            if (width > this.width)
                this.width = width;
            if (height > this.height)
                this.height = height;
            final Object[] d = new Object[width * height];
            for (int y = 0; y < oh; y++)
                System.arraycopy(elementData, y * ow, d, y * width, ow);
            elementData = d;
        }
    }

    private String outOfBoundsMsg(final int x, final int y) {
        return "Index: " + x + "x" + y + ", Size: " + width + "x" + height;
    }

    private void rangeCheck(final int x, final int y) {
        if (x >= width || y >= height)
            throw new IndexOutOfBoundsException(this.outOfBoundsMsg(x, y));
    }

    @Override public int width() { return width; }
    @Override public int height() { return height; }

    @Override
    public E set(final int x, final int y, final E element) {
        ensureCapacity(x + 1, y + 1);
        final int i = y * width + x;
        //noinspection unchecked
        final E old = (E) elementData[i];
        elementData[i] = element;
        return old;
    }

    @Override
    public E get(final int x, final int y) {
        rangeCheck(x, y);
        //noinspection unchecked
        return (E) elementData[y * width + x];
    }

    @Override public void clear() { Arrays.fill(elementData, null); }
}