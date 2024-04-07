package i4l.common;

import java.util.ArrayList;
import java.util.List;

public class I4LNewVar extends I4LOperation {
    public List<String> params = null;

    public List<Var> vars = new ArrayList<>();

    public static class Var {
        public String name = null;
        public Object value = null;
        public Var(final String n, final Object v) {
            name = n;
            value = v;
        }

        @Override
        public String toString() {
            return name + " = " + value;
        }
    }

    @Override
    public String toString() {
        return "NEW_VARS " + params + " : " + vars;
    }
}