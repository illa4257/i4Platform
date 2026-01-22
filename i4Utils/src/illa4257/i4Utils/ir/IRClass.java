package illa4257.i4Utils.ir;

import java.util.ArrayList;

public class IRClass {
    public String name, superName;
    public final ArrayList<IRField> fields = new ArrayList<>();
    public final ArrayList<IRMethod> methods = new ArrayList<>();
}