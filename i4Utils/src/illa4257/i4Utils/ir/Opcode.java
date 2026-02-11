package illa4257.i4Utils.ir;

import java.util.Arrays;
import java.util.List;

public enum Opcode {
    STORE,
    GET_STATIC, PUT_STATIC,
    GET_FIELD, PUT_FIELD,
    ARRAY_GET, ARRAY_SET,

    // Math
    NEGATIVE, ADD, SUBTRACT, MULTIPLY, DIVIDE, REMAINDER, SHIFT_RIGHT, SHIFT_LEFT, UNSIGNED_SHIFT_RIGHT,
    AND, OR, XOR,
    COMPARE, COMPARE_NAN,

    // Conditional
    IF_NULL, IF_NONNULL, IF_EQ, IF_NE,
    IF_LT, IF_LE,
    IF_GT, IF_GE,

    // Control
    ANCHOR,
    ARRAY_LENGTH,
    INVOKE_VIRTUAL,
    INVOKE_SPECIAL,
    INVOKE_STATIC,
    INVOKE_INTERFACE,
    INVOKE_DYNAMIC,
    ALLOCATE,
    NEW_ARRAY,
    RETURN,
    THROW,
    TRY,
    CATCH,
    GOTO,
    MONITOR_ENTER,
    MONITOR_EXIT,

    // Operand Stack related
    POP,
    POP2,
    DUP,
    DUP_x1,
    DUP_x2,
    DUP2,
    DUP2_x1,
    DUP2_x2,
    SWAP,

    CHECK_CAST,
    NO_OP,
    INSTANCEOF,
    INT2LONG,
    LONG2INT,
    DOUBLE2FLOAT,
    DOUBLE2INT,
    DOUBLE2LONG,
    FLOAT2DOUBLE,
    FLOAT2INT,
    FLOAT2LONG,
    INT2BYTE,
    INT2CHAR,
    INT2DOUBLE,
    INT2FLOAT,
    INT2SHORT,
    LONG2DOUBLE,
    LONG2FLOAT,
    HINT;

    static List<Opcode>
            STOPPERS = Arrays.asList(RETURN, THROW),
            IFS2 = Arrays.asList(IF_NULL, IF_NONNULL),
            IFS3 = Arrays.asList(IF_EQ, IF_NE, IF_LT, IF_LE, IF_GT, IF_GT)
            ;
}