package illa4257.i4Utils.ir;

import java.util.Collection;

public enum IRAccess {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    SUPER,
    MODULE,
    STATIC,
    FINAL,
    SYNCHRONIZED,
    VOLATILE,
    NATIVE,
    ANNOTATION, INTERFACE,
    TRANSIENT, ENUM, SYNTHETIC, BRIDGE, VARARGS, STRICT, ABSTRACT;

    public static final short
    /// class, field, method
    ACC_PUBLIC          =         0x0001,
    /// field, method
    ACC_PRIVATE         =         0x0002,
    /// field, method
    ACC_PROTECTED       =         0x0004,
    /// field, method
    ACC_STATIC          =         0x0008,
    /// class, field, method
    ACC_FINAL           =         0x0010,
    /// class
    ACC_SUPER           =         0x0020,
    /// method
    ACC_SYNCHRONIZED    =         0x0020,
    /// field
    ACC_VOLATILE        =         0x0040,
    /// method
    ACC_BRIDGE          =         0x0040,
    /// field
    ACC_TRANSIENT       =         0x0080,
    /// method
    ACC_VARARGS         =         0x0080,
    /// method
    ACC_NATIVE          =         0x0100,
    /// class
    ACC_INTERFACE       =         0x0200,
    /// class, method
    ACC_ABSTRACT        =         0x0400,
    /// method
    ACC_STRICT          =         0x0800,
    /// class, field, method
    ACC_SYNTHETIC       =         0x1000,
    /// class
    ACC_ANNOTATION      =         0x2000,
    /// class, field
    ACC_ENUM            =         0x4000,
    /// class
    ACC_MODULE          = (short) 0x8000;

    public static int toJava(final Collection<IRAccess> accesses) {
        int r = 0;
        for (final IRAccess access : accesses)
            switch (access) {
                case PUBLIC: r |= ACC_PUBLIC;break;
                case PRIVATE: r |= ACC_PRIVATE;break;
                case PROTECTED: r |= ACC_PROTECTED;break;
                case STATIC: r |= ACC_STATIC;break;
                case FINAL: r |= ACC_FINAL;break;
                case SUPER: r |= ACC_SUPER;break;
                case SYNCHRONIZED: r |= ACC_SYNCHRONIZED;break;
                case VOLATILE: r |= ACC_VOLATILE;break;
                case BRIDGE: r |= ACC_BRIDGE;break;
                case TRANSIENT: r |= ACC_TRANSIENT;break;
                case VARARGS: r |= ACC_VARARGS;break;
                case NATIVE: r |= ACC_NATIVE;break;
                case INTERFACE: r |= ACC_INTERFACE;break;
                case ABSTRACT: r |= ACC_ABSTRACT;break;
                case STRICT: r |= ACC_STRICT;break;
                case SYNTHETIC: r |= ACC_SYNTHETIC;break;
                case ANNOTATION: r |= ACC_ANNOTATION;break;
                case ENUM: r |= ACC_ENUM;break;
                case MODULE: r |= ACC_MODULE;break;
            }
        return r;
    }
}