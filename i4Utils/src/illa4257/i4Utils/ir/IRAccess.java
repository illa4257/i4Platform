package illa4257.i4Utils.ir;

import java.util.Collection;

public enum IRAccess {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    VOLATILE,
    NATIVE,
    TRANSIENT, ENUM, SYNTHETIC, BRIDGE, VARARGS, STRICT, ABSTRACT;

    public static int toJava(final Collection<IRAccess> accesses) {
        int r = 0;
        for (final IRAccess access : accesses)
            switch (access) {
                case PUBLIC: r |= 0x0001;break;
                case STATIC: r |= 0x0008;break;
                case VOLATILE: r |= 0x0040;break;
                case TRANSIENT: r |= 0x0080;break;
            }
        return r;
    }
}