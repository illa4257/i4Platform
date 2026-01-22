package illa4257.i4Utils.ir;

public class IRRegister {
    public final int index;

    public IRRegister(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Register(" + index + ')';
    }
}