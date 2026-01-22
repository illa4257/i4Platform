package illa4257.i4Utils.ir;

public class IRExc {
    public String cls;
    public IRAnchor anchor;

    public IRExc() {}

    public IRExc(String cls, IRAnchor anchor) {
        this.cls = cls;
        this.anchor = anchor;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + cls + "," + anchor + "]";
    }
}