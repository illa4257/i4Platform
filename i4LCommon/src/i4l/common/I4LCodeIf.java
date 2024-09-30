package i4l.common;

public class I4LCodeIf {
    public Object check = null, then = null, el = null;

    @Override
    public String toString() {
        return "IF " + check + " ? " + then + " : " + el;
    }
}