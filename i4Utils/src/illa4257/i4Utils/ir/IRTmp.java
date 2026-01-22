package illa4257.i4Utils.ir;

public class IRTmp {
    public final int index;

    public IRTmp(final int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "TMP(" + index + ')';
    }
}