package illa4257.i4Utils.str;

public class CharArraySeq implements CharSequence {
    public char[] chars;
    public int start, end, length;

    public CharArraySeq(final char[] chars) {
        this.chars = chars;
        start = 0;
        end = chars.length;
        length = chars.length;
    }

    public CharArraySeq(final char[] chars, final int start, final int end) {
        this.chars = chars;
        this.start = start;
        this.end = end;
        length = end - start;
    }

    @Override public int length() { return length; }

    @Override
    public char charAt(int i) {
        if (i < 0 || i >= length)
            throw new IndexOutOfBoundsException("Index out of range: " + i);
        return chars[start + i];
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public CharArraySeq subSequence(int start, int end) {
        if (start < 0 || end < 0 || start > end || end > length)
            throw new IndexOutOfBoundsException("Index out of range: start=" + start + ", end=" + end);
        return new CharArraySeq(chars, this.start + start, this.start + end);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return new String(chars, start, length);
    }
}