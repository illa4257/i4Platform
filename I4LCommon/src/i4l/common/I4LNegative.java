package i4l.common;

public class I4LNegative {
    public Object value;

    public I4LNegative(final Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "NEGATIVE " + value;
    }
}