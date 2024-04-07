package i4l.common;

public class I4LCase extends I4LOperation {
    public Object value = null;

    @Override
    public String toString() {
        return "CASE " + value;
    }
}