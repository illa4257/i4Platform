package i4l.common;

import java.util.ArrayList;
import java.util.List;

public class I4LIfElse extends I4LOperation {
    public boolean sub = true;
    public List<Object> begin = null, code = null;

    public I4LIfElse(final I4LStatement s) {
        this.begin = s.begin;
        this.code = s.code;
        this.sub = s.sub;
    }

    public boolean subElse = false;
    public ArrayList<Object> elseCode = new ArrayList<>();

    @Override
    public String toString() {
        return "IF_ELSE " + begin;
    }
}