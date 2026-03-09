package illa4257.i4Utils.ir;

import java.util.ArrayList;

public class IRDynamicMethod {
    public String name = null;
    public IRType type;

    public IRMethodRef factory = null;

    public ArrayList<IRType> params = new ArrayList<>();

    @Override
    public String toString() {
        return "DynamicMethodRef{" + name + params + " -> " + type + ", factory=" + factory + '}';
    }
}