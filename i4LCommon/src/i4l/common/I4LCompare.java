package i4l.common;

public class I4LCompare extends I4LOperation {
    public String check = null;
    public Object v1 = null, v2 = null;

    @Override
    public String toString() {
        return "COMPARE (" + v1 + ") " + check + " (" + v2 + ")";
    }
}