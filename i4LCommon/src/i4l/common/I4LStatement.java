package i4l.common;

import java.util.List;

public class I4LStatement extends I4LOperation {
    public boolean sub = true;
    public String name = null;
    public List<Object> begin = null, code = null;

    @Override
    public String toString() {
        return "STATEMENT " + name + " " + begin;
    }
}