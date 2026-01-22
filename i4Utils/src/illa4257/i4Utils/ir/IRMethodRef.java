package illa4257.i4Utils.ir;

import java.util.ArrayList;

public class IRMethodRef {
    public String cls = null, name = null;
    public IRType type;
    public ArrayList<IRType> params = new ArrayList<>();

    @Override
    public String toString() {
        return "MethodRef{" + cls + '#' + name + params + ',' + type + '}';
    }
}