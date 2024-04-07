package i4l.common;

public class I4LKeyword extends I4LOperation {
    public String name = null;
    public Object operation = null;

    @Override
    public String toString() {
        return "KEYWORD (" + name + ") : (" + operation + ")";
    }
}