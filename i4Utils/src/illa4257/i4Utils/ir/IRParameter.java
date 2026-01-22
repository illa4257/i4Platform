package illa4257.i4Utils.ir;

public class IRParameter {
    public final int index;

    public IRParameter(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Parameter(" + index + ')';
    }
}