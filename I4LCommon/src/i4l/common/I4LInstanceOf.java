package i4l.common;

public class I4LInstanceOf extends I4LOperation {
    public Object object = null;
    public String cn = null;

    @Override
    public String toString() {
        return "INSTANCE_OF " + object + " is " + cn;
    }
}