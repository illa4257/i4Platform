package illa4257.i4Utils.lists;

import java.util.Iterator;

public class CharIterator implements Iterator<Character> {
    public int index = 0;
    public final char[] array;

    public CharIterator(final char[] array) { this.array = array; }
    public CharIterator(final int startIndex, final char[] array) { this.index = startIndex; this.array = array; }

    @Override
    public boolean hasNext() {
        return array.length > index;
    }

    @Override
    public Character next() {
        return array[index++];
    }
}
