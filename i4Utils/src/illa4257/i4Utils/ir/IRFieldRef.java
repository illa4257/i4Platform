package illa4257.i4Utils.ir;

public class IRFieldRef {
    public String cls = null, name = null, returnType = null;

    @Override
    public String toString() {
        return "FieldRef{" + cls + "#" + name + ',' + returnType + '}';
    }
}