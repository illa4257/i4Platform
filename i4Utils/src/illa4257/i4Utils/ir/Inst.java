package illa4257.i4Utils.ir;

public class Inst {
    public Opcode opcode;
    public Object[] params;
    public Object output = null;

    public Inst(final Opcode opcode, final int paramsNumber) {
        this.opcode = opcode;
        this.params = new Object[paramsNumber];
    }

    public Inst(final Opcode opcode, final Object[] params) {
        this.opcode = opcode;
        this.params = params;
    }

    public Inst(final Opcode opcode, final Object output, final Object[] params) {
        this.opcode = opcode;
        this.output = output;
        this.params = params;
    }

    @Override
    public String toString() {
        return "Inst@" + Integer.toHexString(hashCode()) + "(" + opcode + ")";
    }
}