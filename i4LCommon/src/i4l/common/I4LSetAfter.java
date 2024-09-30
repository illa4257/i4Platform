package i4l.common;

public class I4LSetAfter extends I4LOperation {
    public Object path = null;
    public char operation;

    @Override
    public String toString() {
        return "SET_AFTER " + path + operation + operation;
    }
}