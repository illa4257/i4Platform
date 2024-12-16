package i4l.common;

import java.util.List;

public class I4LCall extends I4LOperation {
    public Object target = null;
    public List<Object> args = null;

    public I4LCall() {}
    public I4LCall(final Object target) { this.target = target; }
    public I4LCall(final Object target, final List<Object> args) { this.target = target; this.args = args; }

    @Override
    public String toString() {
        return "CALL " + target + " " + args;
    }
}