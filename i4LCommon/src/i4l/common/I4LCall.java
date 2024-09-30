package i4l.common;

import java.util.List;

public class I4LCall extends I4LOperation {
    public Object target = null;
    public List<Object> args = null;

    @Override
    public String toString() {
        return "CALL " + target + " " + args;
    }
}