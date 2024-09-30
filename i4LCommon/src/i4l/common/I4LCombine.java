package i4l.common;

public class I4LCombine {
    public char operation;

    public I4LCombine(final char ch) {
        operation = ch;
    }

    public Object v1 = null, v2 = null;

    @Override
    public String toString() {
        return "COMBINE (" + v1 + ") " + operation + " (" + v2 + ")";
    }
}