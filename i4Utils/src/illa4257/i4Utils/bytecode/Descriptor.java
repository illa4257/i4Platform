package illa4257.i4Utils.bytecode;

import illa4257.i4Utils.ir.IRType;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class Descriptor {
    public static class Type {
        public static final Type
                    VOID = new Type('V'),
                    BOOL = new Type('Z'),
                    BYTE = new  Type('B'),
                    SHORT = new  Type('S'),
                    CHAR = new  Type('C'),
                    INT = new Type('I'),
                    LONG = new Type('J'),
                    FLOAT = new Type('F'),
                    DOUBLE = new Type('D');

        public final char t;

        public Type(final char t) { this.t = t; }

        public IRType toIRType() {
            int arr;
            Type t = this;
            if (t instanceof Arr) {
                arr = ((Arr) t).lvls;
                t = ((Arr) t).type;
            } else
                arr = 0;
            switch (t.t) {
                case 'V': return new IRType(IRType.Kind.VOID, arr);
                case 'Z': return new IRType(IRType.Kind.BOOLEAN, arr);
                case 'B': return new IRType(IRType.Kind.BYTE, arr);
                case 'S': return new IRType(IRType.Kind.SHORT, arr);
                case 'C': return new IRType(IRType.Kind.CHAR, arr);
                case 'I': return new IRType(IRType.Kind.INT, arr);
                case 'J': return new IRType(IRType.Kind.LONG, arr);
                case 'F': return new IRType(IRType.Kind.FLOAT, arr);
                case 'D': return new IRType(IRType.Kind.DOUBLE, arr);
                case 'L': return new IRType(((Obj) t).ref, arr);
                default: throw new IllegalArgumentException("Unknown Type " + t);
            }
        }
    }

    public static class Obj extends Type {
        public String ref;

        public Obj(final String ref) {
            super('L');
            this.ref = ref;
        }
    }

    public static class Arr extends Type {
        public int lvls = 1;
        public Type type = null;

        public Arr() { super('['); }
    }

    public ArrayList<Type> parameters = new ArrayList<>();
    public Type type;

    public Descriptor(String descriptor) {
        try (final StringReader r = new StringReader(descriptor)) {
            r.mark(1);
            if ((char) r.read() == '(') {
                while (true) {
                    r.mark(1);
                    if ((char) r.read() == ')')
                        break;
                    r.reset();
                    parameters.add(getType(r));
                }
            } else
                r.reset();
            type = getType(r);
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        } catch (final RuntimeException ex) {
            System.err.println("Full: " + descriptor);
            throw ex;
        }
    }

    private static final ThreadLocal<StringBuilder> b = ThreadLocal.withInitial(StringBuilder::new);

    public static Type getType(final StringReader reader) throws IOException {
        final char ch = (char) reader.read();
        switch (ch) {
            case 'V': return Type.VOID;
            case 'Z': return Type.BOOL;
            case 'B': return Type.BYTE;
            case 'S': return Type.SHORT;
            case 'C': return Type.CHAR;
            case 'I': return Type.INT;
            case 'J': return Type.LONG;
            case 'F': return Type.FLOAT;
            case 'D': return Type.DOUBLE;
            case 'L': {
                final StringBuilder b = Descriptor.b.get();
                b.setLength(0);
                while (true) {
                    final char c = (char) reader.read();
                    if (c == ';')
                        break;
                    b.append(c);
                }
                return new Obj(b.toString());
            }
            case '[':
                final Arr arr = new Arr();
                while (true) {
                    reader.mark(1);
                    final char c = (char) reader.read();
                    if (c != '[') {
                        reader.reset();
                        break;
                    }
                    arr.lvls++;
                }
                arr.type = getType(reader);
                return arr;
        }
        throw new RuntimeException("Unknown type " + ch);
    }
}