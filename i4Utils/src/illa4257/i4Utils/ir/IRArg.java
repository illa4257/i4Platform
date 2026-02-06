package illa4257.i4Utils.ir;

public class IRArg {
    public final int index;

    public IRArg(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Parameter(" + index + ')';
    }
}