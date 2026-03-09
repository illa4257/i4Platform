package illa4257.i4Utils.ir;

import illa4257.i4Utils.str.Str;

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
        CHAR, LITERAL
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

    public StringBuilder toJava(final StringBuilder b) {
        Str.repeat(b, "[", array);
        switch (kind) {
            case VOID:
                b.append('V');
                break;
            case BOOLEAN:
                b.append('Z');
                break;
            case BYTE:
                b.append('B');
                break;
            case SHORT:
                b.append('S');
                break;
            case CHAR:
                b.append('C');
                break;
            case INT:
                b.append('I');
                break;
            case LONG:
                b.append('J');
                break;
            case FLOAT:
                b.append('F');
                break;
            case DOUBLE:
                b.append('D');
                break;
            case LITERAL:
                b.append('L').append(cls).append(';');
                break;
            default:
                throw new RuntimeException("Unknown kind: " + kind);
        }
        return b;
    }

    public String toJava() {
        final StringBuilder b = Str.builder();
        try {
            return toJava(b).toString();
        } finally {
            Str.recycle(b);
        }
    }

    @Override
    public String toString() {
        return "IRType(" + kind + (array == 0 ? "" : "[" + array + ']') + (cls != null ? ", " + cls : "") + ")";
    }
}