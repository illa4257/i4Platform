package illa4257.i4Utils.ir;

public class IRType {
    public enum Kind {
        VOID,
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        FLOAT,
        LONG,
        DOUBLE,
        LITERAL
    }

    public boolean isPrimitive = false;
    public int array = 0;
    public Kind kind;
    public String cls;

    public IRType(final String cls) {
        this.kind = Kind.LITERAL;
        this.cls = cls;
    }

    public IRType(final Kind kind) {
        this.kind = kind;
        this.isPrimitive = kind != Kind.LITERAL;
    }

    public IRType(final String cls, final int dimensions) {
        this.kind = Kind.LITERAL;
        this.cls = cls;
        this.array = dimensions;
    }

    public IRType(final Kind kind, final int dimensions) {
        this.kind = kind;
        this.isPrimitive = kind != Kind.LITERAL;
        this.array = dimensions;
    }

    @Override
    public String toString() {
        return "IRType(" + kind + (array == 0 ? "" : "[" + array + ']') + (cls != null ? ", " + cls : "") + ")";
    }
}