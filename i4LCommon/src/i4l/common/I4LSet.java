package i4l.common;

public class I4LSet extends I4LOperation {
    public Object path = null;
    public char operation;
    public Object value = null;

    @Override
    public String toString() {
        return "SET " + path + " " + operation + " " + value;
    }
}