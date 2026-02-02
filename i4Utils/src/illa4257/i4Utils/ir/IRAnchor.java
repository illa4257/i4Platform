package illa4257.i4Utils.ir;

public class IRAnchor {
    public Object id;

    public IRAnchor(final Object id) {
        this.id = id instanceof Short ? (int) (short) id : id;
    }

    @Override
    public String toString() {
        return "Anchor(" + id + ')';
    }
}