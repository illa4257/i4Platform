package i4l.common;

public class I4LMove extends I4LOperation {
    public Object target, steps;
    public String action;

    @Override
    public String toString() {
        return target + " " + action + " " + steps;
    }
}