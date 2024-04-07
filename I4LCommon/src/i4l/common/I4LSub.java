package i4l.common;

public class I4LSub extends I4LOperation {
    public Object object = null;
    public Object path = null;

    @Override
    public String toString() {
        return "SUB (" + object + ") : (" + path + ")";
    }
}