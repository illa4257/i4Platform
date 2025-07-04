package illa4257.i4Utils.bytecode;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class Descriptor {
    public static class Type {
        public static final Type
                    VOID = new Type('V'),
                    INT = new Type('I');

        public final char t;

        public Type(final char t) { this.t = t; }
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
        }
    }

    private static final ThreadLocal<StringBuilder> b = ThreadLocal.withInitial(StringBuilder::new);

    public static Type getType(final StringReader reader) throws IOException {
        final char ch = (char) reader.read();
        switch (ch) {
            case 'V': return Type.VOID;
            case 'I': return Type.INT;
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