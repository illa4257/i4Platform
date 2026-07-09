package illa4257.i4Utils.lists;

import java.util.ArrayList;
import java.util.Arrays;

public class IntSet {
    public static final int[] DEFAULT_SET = new int[0];
    private int[] set;
    private int size;

    public IntSet(final int capacity) {
        this.set = new int[capacity];
        this.size = 0;
    }

    public IntSet() {
        this.set = DEFAULT_SET;
        this.size = 0;

        new ArrayList<>();
    }

    public int size() {
        return size;
    }

    public boolean add(final int n) {
        int index = Arrays.binarySearch(set, 0, size, n);
        if (index >= 0)
            return false;
        index = -index - 1;
        if (size == set.length) {
            final int[] newSet = new int[set.length > 0 ? set.length * 2 : 10];
            System.arraycopy(set, 0, newSet, 0, index);
            System.arraycopy(set, index, newSet, index + 1, size - index);
            set = newSet;
        } else
            System.arraycopy(set, index, set, index + 1, size - index);
        set[index] = n;
        size++;
        return true;
    }

    public boolean contains(final int n) {
        return Arrays.binarySearch(set, 0, size, n) >= 0;
    }

    public boolean remove(final int n) {
        int index = Arrays.binarySearch(set, 0, size, n);
        if (index < 0)
            return false;
        System.arraycopy(set, index + 1, set, index, --size - index);
        return true;
    }

    public void clear() {
        size = 0;
    }

    @Override
    public String toString() {
        int iMax = size - 1;
        if (iMax == -1)
            return "[]";

        final StringBuilder b = new StringBuilder()
                .append('[');
        for (int i = 0; ; i++) {
            b.append(set[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }
}