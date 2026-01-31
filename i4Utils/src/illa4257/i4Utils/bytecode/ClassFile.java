package illa4257.i4Utils.bytecode;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.ir.IRExc;
import illa4257.i4Utils.lists.Iter;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.str.Str;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ClassFile {
    public static final int MAGIC = 0xCAFEBABE;


    public static class IntTag {
        public int n;

        public IntTag(final int n) { this.n = n; }
    }

    public static class LongTag {
        public long n;

        public LongTag(final long n) { this.n = n; }
    }

    public static class FloatTag {
        public float n;

        public FloatTag(final float n) { this.n = n; }
    }

    public static class DoubleTag {
        public double n;

        public DoubleTag(final double n) { this.n = n; }
    }

    public static class StrTag {
        public short stringIndex;

        public StrTag(final short stringIndex) { this.stringIndex = stringIndex; }
    }

    public static class ClsTag {
        public short nameIndex;

        public ClsTag(final short nameIndex) { this.nameIndex = nameIndex; }
    }

    public static class NameAndType {
        public short nameIndex, descriptorIndex;

        public NameAndType(final short nameIndex, final short descriptorIndex) {
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static class Ref {
        public short classIndex, nameAndTypeIndex;

        public Ref(final short classIndex, final short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }
    }

    public static class FieldRef extends Ref {
        public FieldRef(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }
    }

    public static class MethodRef extends Ref {
        public MethodRef(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }
    }

    public static class InterfaceMethodRef extends MethodRef {
        public InterfaceMethodRef(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }
    }

    public static class Field {
        public short accessFlags, nameIndex, descriptorIndex;
        public Collection<Attr> attributes = new ArrayList<>();

        public Field(final short accessFlags, final short nameIndex, final short descriptorIndex) {
            this.accessFlags = accessFlags;
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }
    }

    public static class Method {
        public short accessFlags, nameIndex, descriptorIndex;
        public Collection<Attr> attributes = new ArrayList<>();

        public Method(final short accessFlags, final short nameIndex, final short descriptorIndex) {
            this.accessFlags = accessFlags;
            this.nameIndex = nameIndex;
            this.descriptorIndex = descriptorIndex;
        }

        public static class Exc {
            public int start, end, offset;
            public IRExc exc = new IRExc();
        }

        private static IRType.Kind nType(final int n) {
            switch (n) {
                case 0: return IRType.Kind.INT;
                case 1: return IRType.Kind.LONG;
                case 2: return IRType.Kind.FLOAT;
                case 3: return IRType.Kind.DOUBLE;
            }
            throw new RuntimeException("Unknown type " + n);
        }

        public static boolean isNot64Bit(final Object o) {
            throw new RuntimeException("Unknown object " + o);
        }

        public IRMethod toIRMethod(final ClassFile cf) throws IOException {
            final Descriptor descriptor = new Descriptor((String) cf.constantPool.get(descriptorIndex - 1));
            final IRMethod m = new IRMethod();
            m.name = (String) cf.constantPool.get(nameIndex - 1);
            m.type = descriptor.type.toIRType();
            irAccess(accessFlags, m.access);
            if ((accessFlags & 0x0100) != 0) m.access.add(IRAccess.NATIVE);
            if ((accessFlags & 0x0400) != 0) m.access.add(IRAccess.ABSTRACT);
            for (final Descriptor.Type t : descriptor.parameters)
                m.argumentsTypes.add(t.toIRType());
            for (final Attr attr : attributes) {
                if (!"Code".equals(cf.constantPool.get(attr.nameIndex - 1)))
                    continue;
                final ArrayList<IRAnchor> track = new ArrayList<>();
                final ArrayList<Integer> steps = new ArrayList<>();//, catchOffsets = new ArrayList<>();
                final ArrayList<Exc> excs = new ArrayList<>();
                final ArrayList<Inst> instructions = m.instructions;
                //final SyncVar<ArrayList<Object>> operands = new SyncVar<>(new ArrayList<>());
                final ArrayList<Stack<Object>> activeStacks = new ArrayList<>();
                activeStacks.add(new Stack<>());
                final HashMap<IRAnchor, ArrayList<Stack<Object>>> stacks = new HashMap<>();
                try (final ByteArrayInputStream is = new ByteArrayInputStream(attr.info)) {
                    IO.readBEShort(is); // Max Stack
                    IO.readBEShort(is); // Max Locals

                    int old;
                    final int totalCodeLen;
                    int codeLen = old = totalCodeLen = IO.readBEInt(is), deltaBonus = 0, oldInstSize = 0, oldInstSize2 = 0;
                    is.mark(attr.info.length);
                    //noinspection ResultOfMethodCallIgnored
                    is.skip(totalCodeLen);
                    final int exceptionTableLen = IO.readBEShortI(is);
                    for (int i = 0; i < exceptionTableLen; i++) {
                        final Exc exc = new Exc();
                        exc.start = IO.readBEShortI(is);
                        exc.end = IO.readBEShortI(is);
                        exc.offset = IO.readBEShortI(is);
                        //catchOffsets.add(exc.offset = IO.readBEShortI(is)); // handler
                        final int type = IO.readBEShortI(is); // type
                        if (type != 0)
                            exc.exc.cls = (String) cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(type - 1)).nameIndex - 1);
                        else
                            exc.exc.cls = null;
                        exc.exc.anchor = new IRAnchor(-999);
                        excs.add(exc);
                    }
                    is.reset();
                    for (int i1 = 0, i2 = 0, ot = (m.access.contains(IRAccess.STATIC) ? 0 : 1), t = m.argumentsTypes.size() + ot; i1 < t; i1++, i2++) {
                        instructions.add(new Inst(Opcode.STORE, new IRRegister(i2), new Object[]{ new IRParameter(i1) }));
                        steps.add(0, 0);
                        oldInstSize++;
                        if (i1 < ot)
                            continue;
                        final IRType type = m.argumentsTypes.get(i1 - ot);
                        if ((type.kind == IRType.Kind.LONG || type.kind == IRType.Kind.DOUBLE) && type.array == 0)
                            i2++;
                    }


                    final Consumer<IRAnchor> newBranch = a -> {
                        if (a.id < 1)
                            return;
                        //i4Logger.INSTANCE.w(new RuntimeException("new branch " + activeStacks));
                        final ArrayList<Stack<Object>> vs = new ArrayList<>();
                        for (final Stack<Object> s : activeStacks) {
                            final Stack<Object> s2 = new Stack<>();
                            s2.addAll(s);
                            vs.add(s2);
                        }
                        stacks.put(a, vs);
                    };
                    final Consumer<Object> pushOperand = v -> {
                        if (activeStacks.isEmpty())
                            throw new RuntimeException("Illegal state");
                        for (final Stack<Object> stack : activeStacks)
                            stack.add(v);
                    };
                    final Function<Stack<Object>, Object> pop = stack -> {
                        Object val = stack.pop();
                        if (val instanceof Inst) {
                            final Inst inst = (Inst) val;
                            if (inst.output == null)
                                inst.output = new IRTmp(stack.size());
                            val = inst.output;
                        }

                        return val;
                    };
                    final Supplier<Object> popOperand = () -> {
                        if (activeStacks.size() == 1)
                            return pop.apply(activeStacks.get(0));
                        Iterator<Stack<Object>> iter = activeStacks.iterator();
                        if (!iter.hasNext())
                            throw new RuntimeException("Illegal state");
                        Stack<Object> stack = iter.next();
                        int c = stack.size();

                        int valIndex = -1;
                        while (iter.hasNext()) {
                            stack = iter.next();
                            if (c != stack.size())
                                throw new RuntimeException("Not implemented " + activeStacks);
                            final Object v = stack.get(c - 1);
                            if (v instanceof IRTmp) {
                                if (valIndex == -1)
                                    valIndex = ((IRTmp) v).index;
                                if (((IRTmp) v).index != valIndex)
                                    throw new RuntimeException("Not implemented " + activeStacks);
                                continue;
                            }
                            if (v instanceof Inst) {
                                if (((Inst) v).output != null)
                                    throw new RuntimeException("Not implemented " + activeStacks);
                                continue;
                            }
                            throw new RuntimeException("Not implemented " + activeStacks);
                        }
                        c--;
                        if (valIndex != c && valIndex != -1)
                            throw new RuntimeException("Not implemented " + activeStacks);
                        final ArrayList<Object> used = new ArrayList<>();
                        iter = activeStacks.iterator();
                        while (iter.hasNext()) {
                            stack = iter.next();
                            final Object v = stack.pop();
                            if (used.contains(v))
                                continue;
                            used.add(v);
                            if (v instanceof Inst)
                                ((Inst) v).output = new IRTmp(c);
                        }
                        return new IRTmp(c);
                    };
                    
                    for (; codeLen != 0; codeLen--) {
                        final int currentPC = totalCodeLen - codeLen;
                        for (final Exc exc : Iter.reversible(excs))
                            if (exc.start == currentPC) {
                                instructions.add(new Inst(Opcode.TRY, new Object[] { exc.exc }));
                                steps.add(0, 0);
                                oldInstSize++;
                            }
                        for (final Exc exc : excs)
                            if (exc.end == currentPC) {
                                instructions.add(new Inst(Opcode.CATCH, new Object[] { exc.exc }));
                                steps.add(0, 0);
                                oldInstSize++;
                            }
                        for (final Exc exc : excs)
                            if (exc.offset == currentPC) {
                                instructions.add(new Inst(Opcode.ANCHOR, new Object[] { exc.exc.anchor.id = instructions.size() }));
                                steps.add(0, 0);
                                oldInstSize++;
                                activeStacks.clear();
                                activeStacks.add(new Stack<>());
                                pushOperand.accept(Const.EXCEPTION);
                            }
                        final int b = is.read();
                        switch (b) {
                            case 0: { // NOOP
                                instructions.add(new Inst(Opcode.NO_OP, 0));
                                break;
                            }
                            case 1: // null
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{null});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }
                            case 2: // iconst_m1
                            case 3: // iconst_0
                            case 4: // iconst_1
                            case 5: // iconst_2
                            case 6: // iconst_3
                            case 7: // iconst_4
                            case 8: // iconst_5
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRInt(b - 3)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 9: // lconst_0
                            case 10: // lconst_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRLong(b - 9)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 11: // fconst_0
                            case 12: // fconst_1
                            case 13: // fconst_2
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRFloat(b - 11)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 14: // dconst_0
                            case 15: // dconst_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRDouble(b - 14)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 16: // bipush
                            {
                                codeLen--;
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRByte(IO.readByte(is))});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 17: // sipush
                            {
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRShort(IO.readBEShort(is))});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 18: // ldc
                            {
                                codeLen--;
                                Object v = cf.constantPool.get(IO.readByteI(is) - 1);
                                if (v instanceof ClsTag)
                                    v = cf.constantPool.get(((ClsTag) v).nameIndex - 1);
                                else if (v instanceof IntTag)
                                    v = new IRInt(((IntTag) v).n);
                                else if (v instanceof LongTag)
                                    v = new IRLong(((LongTag) v).n);
                                else if (v instanceof FloatTag)
                                    v = new IRFloat(((FloatTag) v).n);
                                else if (v instanceof DoubleTag)
                                    v = new IRDouble(((DoubleTag) v).n);
                                else if (v instanceof StrTag)
                                    v = cf.constantPool.get(((StrTag) v).stringIndex - 1);
                                else
                                    throw new RuntimeException("Unknown type of constants: " + v);
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{v});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 20: // ldc2_w
                            {
                                codeLen -= 2;
                                Object v = cf.constantPool.get(IO.readBEShortI(is) - 1);
                                if (v instanceof ClsTag)
                                    v = cf.constantPool.get(((ClassFile.StrTag) v).stringIndex - 1);
                                else if (v instanceof IntTag)
                                    v = new IRInt(((IntTag) v).n);
                                else if (v instanceof LongTag)
                                    v = new IRLong(((LongTag) v).n);
                                else if (v instanceof FloatTag)
                                    v = new IRFloat(((FloatTag) v).n);
                                else if (v instanceof DoubleTag)
                                    v = new IRDouble(((DoubleTag) v).n);
                                else if (v instanceof StrTag)
                                    v = cf.constantPool.get(((ClassFile.StrTag) v).stringIndex - 1);
                                else
                                    throw new RuntimeException("Unknown type of constants: " + v);
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{v});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 21: // iload
                            case 22: // lload
                            case 23: // fload
                            case 24: // dload
                            case 25: // aload
                            {
                                codeLen--;
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(IO.readByteI(is))});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 26: // iload_0
                            case 27: // iload_1
                            case 28: // iload_2
                            case 29: // iload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 26)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 30: // lload_0
                            case 31: // lload_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 30)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 34: // fload_0
                            case 35: // fload_1
                            case 36: // fload_2
                            case 37: // fload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 34)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 38: // dload_0
                            case 39: // dload_1
                            case 40: // dload_2
                            case 41: // dload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 38)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 42: // aload_0
                            case 43: // aload_1
                            case 44: // aload_2
                            case 45: // aload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 42)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 46: // iaload
                            case 48: // faload
                            case 49: // daload
                            case 50: // aaload
                            case 51: // baload
                            case 52: // caload
                            {
                                final Object index = popOperand.get();
                                final Inst inst = new Inst(Opcode.ARRAY_GET, new Object[]{popOperand.get(), index});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 54: // istore
                            case 55: // lstore
                            case 56: // fstore
                            case 57: // dstore
                            case 58: // astore
                            {
                                codeLen--;
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(IO.readByteI(is)), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 59: // istore_0
                            case 60: // istore_1
                            case 61: // istore_2
                            case 62: // istore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 59), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 63: // lstore_0
                            case 64: // lstore_1
                            case 65: // lstore_2
                            case 66: // lstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 63), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 67: // fstore_0
                            case 68: // fstore_1
                            case 69: // fstore_2
                            case 70: // fstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 67), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 71: // dstore_0
                            case 72: // dstore_1
                            case 73: // dstore_2
                            case 74: // dstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 72), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 75: // astore_0
                            case 76: // astore_1
                            case 77: // astore_2
                            case 78: // astore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 75), new Object[]{popOperand.get()}));
                                break;
                            }

                            case 79: // iastore
                            case 80: // lastore
                            case 81: // fastore
                            case 82: // dastore
                            case 83: // aastore
                            case 84: // bastore
                            case 85: // castore
                            {
                                final Object val = popOperand.get(), index = popOperand.get();
                                instructions.add(new Inst(Opcode.ARRAY_SET, null, new Object[]{popOperand.get(), index, val}));
                                break;
                            }

                            case 88: { // pop2
                                /*final Object r = activeStacks.get(0).remove(activeStacks.get(0).size() - 1); // TODO: fix
                                if (r == Const.LONG_RESULT) {
                                    last.remove(last.size() - 1);
                                    break;
                                }
                                if (r == Const.RESULT) {
                                    final Inst inst = last.remove(last.size() - 1);
                                    final IRMethodRef ref = (IRMethodRef) inst.params[0];
                                    if ((ref.type.kind == IRType.Kind.LONG || ref.type.kind == IRType.Kind.DOUBLE) && ref.type.array == 0)
                                        break;
                                }*/
                                if (activeStacks.isEmpty())
                                    throw new RuntimeException("Illegal state");
                                for (final Stack<Object> stack : activeStacks)
                                    if (isNot64Bit(stack.pop()))
                                        stack.pop();
                                break;
                            }
                            case 87: { // pop
                                /*final Object r = activeStacks.get(0).remove(activeStacks.get(0).size() - 1); // TODO: fix
                                if (r == Const.LONG_RESULT) {
                                    pushOperand.accept(Const.RESULT);
                                    break;
                                }
                                if (r == Const.RESULT)
                                    last.remove(last.size() - 1);*/
                                if (activeStacks.isEmpty())
                                    throw new RuntimeException("Illegal state");
                                for (final Stack<Object> stack : activeStacks)
                                    stack.pop();
                                break;
                            }

                            case 89: { // dup
                                Object val = popOperand.get();
                                pushOperand.accept(val);
                                pushOperand.accept(val);
                                break;
                            }

                            case 90: // dup_x1
                            {
                                final Object d = popOperand.get(), o = popOperand.get();
                                pushOperand.accept(d);
                                pushOperand.accept(o);
                                pushOperand.accept(d);
                                break;
                            }

                            case 91: // dup_x2
                            {
                                final Object d = popOperand.get(), o1 = popOperand.get(), o2 = popOperand.get();
                                pushOperand.accept(d);
                                pushOperand.accept(o1);
                                pushOperand.accept(o2);
                                pushOperand.accept(d);
                                break;
                            }

                            case 92: // dup2
                            {
                                final Object v1 = popOperand.get();
                                if (isNot64Bit(v1)) {
                                    final Object v2 = popOperand.get();
                                    pushOperand.accept(v2);
                                    pushOperand.accept(v1);
                                    pushOperand.accept(v2);
                                } else
                                    pushOperand.accept(v1);
                                pushOperand.accept(v1);
                                break;
                            }

                            case 93: // dup2_x1
                            {
                                final Object v1 = popOperand.get(), v2 = popOperand.get();
                                if (isNot64Bit(v1)) {
                                    final Object v3 = popOperand.get();
                                    pushOperand.accept(v2);
                                    pushOperand.accept(v1);
                                    pushOperand.accept(v3);
                                } else
                                    pushOperand.accept(v1);
                                pushOperand.accept(v2);
                                pushOperand.accept(v1);
                                break;
                            }

                            case 94: // dup2_x2
                            {
                                final Object v1 = popOperand.get(), v2 = popOperand.get();
                                if (isNot64Bit(v1)) {
                                    if (isNot64Bit(v2)) {
                                        final Object v3 = popOperand.get();
                                        if (isNot64Bit(v3)) {
                                            final Object v4 = popOperand.get();
                                            if (isNot64Bit(v4)) { // Form 1
                                                pushOperand.accept(v2);
                                                pushOperand.accept(v1);
                                                pushOperand.accept(v4);
                                            } else
                                                throw new RuntimeException("Illegal state");
                                        } else { // Form 3
                                            pushOperand.accept(v2);
                                            pushOperand.accept(v1);
                                        }
                                        pushOperand.accept(v3);
                                    } else
                                        throw new RuntimeException("Illegal state");
                                } else if (isNot64Bit(v2)) {
                                    final Object v3 = popOperand.get();
                                    if (isNot64Bit(v3)) { // Form 2
                                        pushOperand.accept(v1);
                                        pushOperand.accept(v3);
                                    } else
                                        throw new RuntimeException("Illegal state");
                                } else // Form 4
                                    pushOperand.accept(v1);
                                pushOperand.accept(v2);
                                pushOperand.accept(v1);
                                break;
                            }

                            case 96: // iadd
                            case 97: // ladd
                            case 98: // fadd
                            case 99: // dadd
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.ADD, new Object[]{popOperand.get(), val2,
                                        nType(b - 96)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 100: // isub
                            case 101: // lsub
                            case 102: // fsub
                            case 103: // dsub
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.SUBTRACT, new Object[]{ popOperand.get(), val2, nType(b - 100) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 104: // imul
                            case 105: // lmul
                            case 106: // fmul
                            case 107: // dmul
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.MULTIPLY, new Object[] { popOperand.get(), val2, nType(b - 104) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 108: // idiv
                            case 109: // ldiv
                            case 110: // fdiv
                            case 111: // ddiv
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.DIVIDE, new Object[] { popOperand.get(), val2, nType(b - 108) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 112: // irem
                            case 113: // lrem
                            case 114: // frem
                            case 115: // drem
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.REMAINDER, new Object[] { popOperand.get(), val2, nType(b - 112) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 116: // ineg
                            case 118: // fneg
                            case 119: // dneg
                            {
                                final Inst inst = new Inst(Opcode.NEGATIVE, new Object[] { popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 120: // ishl
                            case 121: // lshl
                            {
                                final Object s = popOperand.get();
                                final Inst inst = new Inst(Opcode.SHIFT_LEFT, new Object[] { popOperand.get(), s, nType(b - 120) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 122: // ishr
                            case 123: // lshr
                            {
                                final Object s = popOperand.get();
                                final Inst inst = new Inst(Opcode.SHIFT_RIGHT, new Object[] { popOperand.get(), s, nType(b - 122) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 126: // iand
                            case 128: // ior
                            case 130: // ixor
                            {
                                final Object v2 = popOperand.get();
                                final Inst inst = new Inst(b == 126 ? Opcode.AND : b == 128 ? Opcode.OR : Opcode.XOR, new Object[] { popOperand.get(), v2 });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 132: // iinc
                            {
                                codeLen -= 2;
                                final int i = IO.readByteI(is);
                                instructions.add(new Inst(Opcode.ADD, new IRRegister(i), new Object[] { new IRRegister(i), new IRInt(IO.readByteI(is)), IRType.Kind.INT }));
                                break;
                            }

                            case 133: // i2l
                            {
                                final Inst inst = new Inst(Opcode.INT2LONG, new Object[] { popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 134: // i2f
                            {
                                final Inst inst = new Inst(Opcode.INT2FLOAT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 135: // i2d
                            {
                                final Inst inst = new Inst(Opcode.INT2DOUBLE, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 136: // l2i
                            {
                                final Inst inst = new Inst(Opcode.LONG2INT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 139: // f2i
                            {
                                final Inst inst = new Inst(Opcode.FLOAT2INT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 140: // f2l
                            {
                                final Inst inst = new Inst(Opcode.FLOAT2LONG, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 141: // f2d
                            {
                                final Inst inst = new Inst(Opcode.FLOAT2DOUBLE, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 142: // d2i
                            {
                                final Inst inst = new Inst(Opcode.DOUBLE2INT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 143: // d2l
                            {
                                final Inst inst = new Inst(Opcode.DOUBLE2LONG, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 144: // d2f
                            {
                                final Inst inst = new Inst(Opcode.DOUBLE2FLOAT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 145: // i2b
                            {
                                final Inst inst = new Inst(Opcode.INT2BYTE, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 146: // i2c
                            {
                                final Inst inst = new Inst(Opcode.INT2CHAR, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 147: // i2s
                            {
                                final Inst inst = new Inst(Opcode.INT2SHORT, new Object[]{popOperand.get()});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 148: // lcmp
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.COMPARE, new Object[]{popOperand.get(), val2});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 149: // fcmpl
                            case 150: // fcmpg
                            case 151: // dcmpl
                            case 152: // dcmpg
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.COMPARE_NAN, new Object[]{popOperand.get(), val2, b == 149 || b == 151 ? new IRInt(-1) : new IRInt(1)});
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 153: // if_eq
                            case 154: // if_ne
                            case 155: // if_lt
                            case 156: // if_ge
                            case 157: // if_gt
                            case 158: // if_le
                            {
                                codeLen -= 2;
                                final IRAnchor bn = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(
                                        b == 153 ? Opcode.IF_EQ :
                                        b == 154 ? Opcode.IF_NE :
                                        b == 155 ? Opcode.IF_LT :
                                        b == 156 ? Opcode.IF_GE :
                                        b == 157 ? Opcode.IF_GT :
                                                Opcode.IF_LE
                                        , new Object[] { popOperand.get(), new IRInt(0), bn }));
                                track.add(bn);
                                break;
                            }

                            case 159: // if_icmpeq
                            case 160: // if_icmpne
                            case 161: // if_icmplt
                            case 162: // if_icmpge
                            case 163: // if_icmpgt
                            case 164: // if_icmple

                            case 165: // if_acmpeq
                            case 166: // if_acmpne
                            {
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                Object val2 = popOperand.get();
                                instructions.add(new Inst(
                                        b == 159 || b == 165 ? Opcode.IF_EQ :
                                                b == 160 || b == 166 ? Opcode.IF_NE :
                                                        b == 161 ? Opcode.IF_LT :
                                                                b == 162 ? Opcode.IF_GE :
                                                                        b == 163 ? Opcode.IF_GT :
                                                                                Opcode.IF_LE
                                        , new Object[] { popOperand.get(), val2, skip }));
                                track.add(skip);
                                break;
                            }

                            case 167: { // goto
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { skip }));
                                track.add(skip);
                                break;
                            }

                            case 168: { // jsr
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { skip }));
                                track.add(skip);
                                break;
                            }

                            case 172: // ireturn
                            case 173: // lreturn
                            case 174: // freturn
                            case 175: // dreturn
                            case 176: // areturn
                            {
                                instructions.add(new Inst(Opcode.RETURN, new Object[]{ popOperand.get() }));
                                activeStacks.clear();
                                break;
                            }

                            case 177: // return
                                activeStacks.clear();
                                instructions.add(new Inst(Opcode.RETURN, 0));
                                break;

                            case 178: { // getstatic
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.GET_STATIC, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 179: { // putstatic
                                codeLen -= 2;
                                instructions.add(new Inst(Opcode.PUT_STATIC, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)), popOperand.get() }));
                                break;
                            }

                            case 180: // getfield
                            {
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.GET_FIELD, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)), popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 181: {
                                codeLen -= 2; // putfield
                                Object val = popOperand.get();
                                instructions.add(new Inst(Opcode.PUT_FIELD, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)), popOperand.get(), val }));
                                break;
                            }

                            case 182: // invokevirtual
                            case 183: // invokespecial
                            case 184: // invokestatic
                            {
                                codeLen -= 2;
                                final ClassFile.MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                final int l = d.parameters.size() + (b == 184 ? 1 : 2);
                                final Inst inst = new Inst(b == 182 ? Opcode.INVOKE_VIRTUAL : b == 183 ? Opcode.INVOKE_SPECIAL : Opcode.INVOKE_STATIC, l);
                                inst.params[0] = methodRef(cf, ref);
                                for (int i = l - 1; i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID)
                                    pushOperand.accept(inst);
                                break;
                            }

                            case 185: // invokeinterface
                            {
                                codeLen -= 4;
                                final InterfaceMethodRef ref = (ClassFile.InterfaceMethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                IO.readByteI(is); // stack size
                                if (IO.readByte(is) != 0)
                                    throw new RuntimeException("Not valid the invoke interface instruction.");
                                final Inst inst = new Inst(Opcode.INVOKE_INTERFACE, d.parameters.size() + 2);
                                inst.params[0] = methodRef(cf, ref);
                                for (int i = d.parameters.size() + 1; i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID)
                                    pushOperand.accept(inst);
                                break;
                            }

                            case 186: // invokedynamic
                            {
                                codeLen -= 4;
                                final MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                if (IO.readByte(is) != 0 || IO.readByte(is) != 0)
                                    throw new RuntimeException("Not valid the invoke interface instruction.");
                                final Inst inst = new Inst(Opcode.INVOKE_DYNAMIC, d.parameters.size() + 2);
                                inst.params[0] = methodRef(cf, ref);
                                for (int i = d.parameters.size(); i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID)
                                    pushOperand.accept(inst);
                                break;
                            }

                            case 187: { // new
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.ALLOCATE, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 188: { // newarray
                                codeLen--;
                                final byte t = IO.readByte(is);
                                IRType.Kind kind;
                                switch (t) {
                                    case 4: kind = IRType.Kind.BOOLEAN; break;
                                    case 5: kind = IRType.Kind.CHAR; break;
                                    case 6: kind = IRType.Kind.FLOAT; break;
                                    case 7: kind = IRType.Kind.DOUBLE; break;
                                    case 8: kind = IRType.Kind.BYTE; break;
                                    case 9: kind = IRType.Kind.SHORT; break;
                                    case 10: kind = IRType.Kind.INT; break;
                                    case 11: kind = IRType.Kind.LONG; break;
                                    default: throw new RuntimeException("Unknown kind for a new array " + t);
                                }
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, new Object[] { kind, popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 189: { // anewarray
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShortI(is) - 1)).nameIndex - 1), popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 190: { // arraylength
                                final Inst inst = new Inst(Opcode.ARRAY_LENGTH, new Object[]{ popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 191: { // athrow
                                instructions.add(new Inst(Opcode.THROW, new Object[]{ popOperand.get() }));
                                activeStacks.clear();
                                // TODO: I removed operands.get().clear();
                                break;
                            }

                            case 192: { // checkcast
                                codeLen -= 2;
                                final Object operand = popOperand.get();
                                pushOperand.accept(operand);
                                instructions.add(new Inst(Opcode.CHECK_CAST, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) }));
                                break;
                            }

                            case 193: { // instanceof
                                codeLen -= 2;
                                final Object operand = popOperand.get();
                                final Inst inst = new Inst(Opcode.INSTANCEOF, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                pushOperand.accept(inst);
                                break;
                            }

                            case 194: // monitorenter
                            case 195: // monitorexit
                            {
                                instructions.add(new Inst(b == 194 ? Opcode.MONITOR_ENTER : Opcode.MONITOR_EXIT, new Object[] { popOperand.get() }));
                                break;
                            }

                            case 198: // if_null
                            case 199: // if_nonnull
                            {
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(b == 198 ? Opcode.IF_NULL : Opcode.IF_NONNULL, new Object[] { popOperand.get(), skip }));
                                track.add(skip);
                                break;
                            }

                            case 200: { // goto_w
                                codeLen -= 4;
                                final IRAnchor skip = new IRAnchor(IO.readBEInt(is));
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { skip }));
                                track.add(skip);
                                break;
                            }

                            default:
                                throw new RuntimeException("Unknown operation: " + b);
                        }

                        final int delta = old - codeLen;
                        old = codeLen;
                        {
                            final Iterator<IRAnchor> iter = track.iterator();
                            final int instDelta = instructions.size() - oldInstSize;
                            oldInstSize = instructions.size();
                            m:
                            while (iter.hasNext()) {
                                final IRAnchor n = iter.next();
                                if (n.id < 0) {
                                    for (int i = 0; i < steps.size(); i++) {
                                        n.id += steps.get(i);
                                        if (n.id == 0) {
                                            iter.remove();
                                            final Inst anchor = instructions.get(instructions.size() - 3 - i);
                                            if (anchor.opcode == Opcode.ANCHOR) {
                                                n.id = (int) anchor.params[0];
                                                continue m;
                                            }
                                            n.id = instructions.size() - 2 - i;
                                            instructions.add(n.id, new Inst(Opcode.ANCHOR, new Object[]{n.id}));
                                            steps.add(steps.size() - n.id, 0);
                                            oldInstSize++;
                                            continue m;
                                        } else if (n.id > 0)
                                            throw new RuntimeException("Too much: " + n.id + " | " + (instructions.size() - n.id) + " | " + instructions.size() + " vs " + steps.size() + " | " + steps);
                                    }
                                    throw new RuntimeException("Illegal step amount " + n.id + " vs " + steps);
                                }
                                n.id -= delta;
                                if (n.id == 0) {
                                    iter.remove();
                                    if (!instructions.isEmpty()) {
                                        final Inst anchor = instructions.get(instructions.size() - 1);
                                        if (anchor.opcode == Opcode.ANCHOR) {
                                            n.id = (int) anchor.params[0];
                                            continue;
                                        }
                                    }
                                    n.id = instructions.size();
                                    instructions.add(new Inst(Opcode.ANCHOR, new Object[]{n.id}));
                                    steps.add(0, 0);
                                    oldInstSize++;
                                }
                            }
                            if (instDelta == 0)
                                deltaBonus += delta;
                            else if (instDelta == 1) {
                                steps.add(0, delta + deltaBonus);
                                deltaBonus = 0;
                            } else
                                throw new RuntimeException("Too much" + instDelta);
                        }
                        for (; oldInstSize2 < instructions.size(); oldInstSize2++) {
                            final Inst inst = instructions.get(oldInstSize2);
                            if (
                                    Arrays.asList(
                                            Opcode.IF_EQ, Opcode.IF_NE,
                                            Opcode.IF_LT, Opcode.IF_LE,
                                            Opcode.IF_GT, Opcode.IF_GE
                                    ).contains(inst.opcode)
                            ) {
                                newBranch.accept((IRAnchor) inst.params[2]);
                                continue;
                            }
                            if (
                                    Arrays.asList(
                                            Opcode.IF_NULL, Opcode.IF_NONNULL
                                    ).contains(inst.opcode)
                            ) {
                                newBranch.accept((IRAnchor) inst.params[1]);
                                continue;
                            }
                            if (inst.opcode == Opcode.GOTO) {
                                newBranch.accept((IRAnchor) inst.params[0]);
                                activeStacks.clear();
                                continue;
                            }
                        }
                        final ArrayList<IRAnchor> toBeRemoved = new ArrayList<>();
                        for (final Map.Entry<IRAnchor, ArrayList<Stack<Object>>> e : stacks.entrySet()) {
                            if (track.contains(e.getKey()))
                                continue;
                            activeStacks.addAll(e.getValue());
                            toBeRemoved.add(e.getKey());
                        }
                        for (final IRAnchor a : toBeRemoved)
                            stacks.remove(a);
                        if (activeStacks.size() > 1) {
                            final Iterator<Stack<Object>> iter = activeStacks.iterator();
                            if (iter.hasNext()) {
                                Stack<Object> stack, stack2;
                                int i = 1;
                                for (stack = iter.next(); iter.hasNext(); stack = iter.next(), i++)
                                    deduplicateStacks:
                                    for (int j = i; j < activeStacks.size(); j++) {
                                        stack2 = activeStacks.get(j);
                                        if (stack.size() != stack2.size())
                                            continue;
                                        final Iterator<Object> s1 = stack.iterator(), s2 = stack2.iterator();
                                        while (s1.hasNext())
                                            if (!Objects.equals(s1.next(), s2.next()))
                                                continue deduplicateStacks;
                                        iter.remove();
                                        i--;
                                        break;
                                    }
                            }
                        }
                    }
                    if (!track.isEmpty())
                        throw new RuntimeException("Some tracks left: " + track);
                } catch (final RuntimeException e) {
                    i4Logger.INSTANCE.e(m.name);
                    m.print();
                    throw e;
                }
            }
            return m;
        }

        public static IRFieldRef fieldRef(final ClassFile cf, final ClassFile.FieldRef ref) {
            final IRFieldRef ir = new IRFieldRef();
            final ClassFile.ClsTag cls = (ClassFile.ClsTag) cf.constantPool.get(ref.classIndex - 1);
            ir.cls = (String) cf.constantPool.get(cls.nameIndex - 1);
            final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
            ir.name = (String) cf.constantPool.get(nt.nameIndex - 1);
            final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
            ir.returnType = type(d.type);
            return ir;
        }

        public static IRMethodRef methodRef(final ClassFile cf, final ClassFile.MethodRef ref) {
            final IRMethodRef ir = new IRMethodRef();
            final ClassFile.ClsTag cls = (ClassFile.ClsTag) cf.constantPool.get(ref.classIndex - 1);
            ir.cls = (String) cf.constantPool.get(cls.nameIndex - 1);
            final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
            ir.name = (String) cf.constantPool.get(nt.nameIndex - 1);
            final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
            for (final Descriptor.Type t : d.parameters)
                ir.params.add(t.toIRType());
            ir.type = d.type.toIRType();
            return ir;
        }

        public static String type(final Descriptor.Type t) {
            if (t == Descriptor.Type.LONG)
                return "long";
            if (t == Descriptor.Type.INT)
                return "int";
            if (t == Descriptor.Type.VOID)
                return "void";
            if (t == Descriptor.Type.BOOL)
                return "boolean";
            if (t.t == 'L')
                return ((Descriptor.Obj) t).ref;
            if (t.t == '[') {
                Descriptor.Arr arr = (Descriptor.Arr) t;
                return type(arr.type) + Str.repeat("[", arr.lvls);
            }
            throw new RuntimeException("Unknown type: " + t.t);
        }
    }

    public static ClassFile parse(final InputStream inputStream) throws IOException {
        if (IO.readBEInt(inputStream) != MAGIC)
            throw new UnsupportedOperationException("Not a class file");

        final ClassFile cls = new ClassFile();
        cls.minorVersion = IO.readBEShort(inputStream);
        cls.majorVersion = IO.readBEShort(inputStream);

        short poolSize = IO.readBEShort(inputStream);
        for (poolSize--; poolSize != 0; poolSize--) {
            final byte tag = IO.readByte(inputStream);
            switch (tag) {
                case 1:
                    cls.constantPool.add(new String(IO.readByteArray(inputStream, IO.readBEShort(inputStream)),
                            StandardCharsets.UTF_8));
                    break;
                case 3:
                    cls.constantPool.add(new IntTag(IO.readBEInt(inputStream)));
                    break;
                case 4:
                    cls.constantPool.add(new FloatTag(IO.readBEFloat(inputStream)));
                    break;
                case 5:
                    cls.constantPool.add(new LongTag(IO.readBELong(inputStream)));
                    cls.constantPool.add(null);
                    poolSize--;
                    break;
                case 6:
                    cls.constantPool.add(new DoubleTag(IO.readBEDouble(inputStream)));
                    cls.constantPool.add(null);
                    poolSize--;
                    break;
                case 7:
                    cls.constantPool.add(new ClsTag(IO.readBEShort(inputStream)));
                    break;
                case 8:
                    cls.constantPool.add(new StrTag(IO.readBEShort(inputStream)));
                    break;
                case 9:
                    cls.constantPool.add(new FieldRef(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                case 10:
                    cls.constantPool.add(new MethodRef(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                case 11: // InterfaceMethodRef
                    cls.constantPool.add(new InterfaceMethodRef(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                case 12:
                    cls.constantPool.add(new NameAndType(IO.readBEShort(inputStream), IO.readBEShort(inputStream)));
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown tag: " + tag);
            }
        }

        cls.accessFlags = IO.readBEShort(inputStream);
        cls.thisIndex = IO.readBEShort(inputStream);
        cls.superIndex = IO.readBEShort(inputStream);

        for (int i = IO.readBEShortI(inputStream); i > 0; i--)
            cls.interfaces.add(IO.readBEShortI(inputStream));

        for (short fieldsCount = IO.readBEShort(inputStream); fieldsCount != 0; fieldsCount--) {
            final Field f = new Field(IO.readBEShort(inputStream), IO.readBEShort(inputStream), IO.readBEShort(inputStream));
            for (short attributesCount = IO.readBEShort(inputStream); attributesCount != 0; attributesCount--)
                f.attributes.add(new Attr(IO.readBEShort(inputStream), IO.readByteArray(inputStream, IO.readBEInt(inputStream))));
            cls.fields.add(f);
        }

        for (short methodsCount = IO.readBEShort(inputStream); methodsCount != 0; methodsCount--) {
            final Method m = new Method(IO.readBEShort(inputStream), IO.readBEShort(inputStream), IO.readBEShort(inputStream));
            for (short attributesCount = IO.readBEShort(inputStream); attributesCount != 0; attributesCount--)
                m.attributes.add(new Attr(IO.readBEShort(inputStream), IO.readByteArray(inputStream, IO.readBEInt(inputStream))));
            cls.methods.add(m);
        }

        for (short attributesCount = IO.readBEShort(inputStream); attributesCount != 0; attributesCount--) {
            final short nameIndex = IO.readBEShort(inputStream);
            final int len = IO.readBEInt(inputStream);
            if ("SourceFile".equals(cls.constantPool.get(nameIndex - 1)))
                cls.fileNameIndex = IO.readBEShort(inputStream);
            else
                cls.attributes.add(new Attr(nameIndex, IO.readByteArray(inputStream, len)));
        }

        return cls;
    }

    public short majorVersion = 0, minorVersion = 0, accessFlags = 0, thisIndex = -1, superIndex = -1, fileNameIndex = -1;
    public ArrayList<Object> constantPool = new ArrayList<>();
    public ArrayList<Integer> interfaces = new  ArrayList<>();
    public Collection<Attr> attributes = new ArrayList<>();
    public Collection<Field> fields = new ArrayList<>();
    public Collection<Method> methods = new ArrayList<>();

    public IRClass toIRClass() throws IOException {
        IRClass cls = new IRClass();
        cls.name = (String) constantPool.get(((ClsTag) constantPool.get(thisIndex - 1)).nameIndex - 1);
        cls.superName = superIndex != 0 ? (String) constantPool.get(((ClsTag) constantPool.get(superIndex - 1)).nameIndex - 1) : null;
        for (final Field f : fields) {
            final IRField field = new IRField();
            field.name = (String) constantPool.get(f.nameIndex - 1);
            final Descriptor d = new Descriptor((String) constantPool.get(f.descriptorIndex - 1));
            field.type = d.type.toIRType();
            cls.fields.add(field);
        }
        for (final Method m : methods)
            cls.methods.add(m.toIRMethod(this));
        return cls;
    }

    public static void irAccess(final short mask, final ArrayList<IRAccess> access) {
        if ((mask & 0x001) != 0) access.add(IRAccess.PUBLIC);
        if ((mask & 0x002) != 0) access.add(IRAccess.PRIVATE);
        if ((mask & 0x004) != 0) access.add(IRAccess.PROTECTED);
        if ((mask & 0x008) != 0) access.add(IRAccess.STATIC);
        if ((mask & 0x010) != 0) access.add(IRAccess.FINAL);
    }
}