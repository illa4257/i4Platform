package illa4257.i4Utils.ir;

import java.util.ArrayList;

public class IRField {
    public String name;
    public IRType type;
    public Object value = null;
    public final ArrayList<IRAccess> access = new ArrayList<>();
}