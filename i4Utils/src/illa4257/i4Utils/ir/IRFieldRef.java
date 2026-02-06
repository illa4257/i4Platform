package illa4257.i4Utils.ir;

public class IRFieldRef {
    public String cls = null, name = null;
    public IRType type = null;

    @Override
    public String toString() {
        return "FieldRef{" + cls + "#" + name + ',' + type + '}';
    }
}