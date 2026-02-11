package illa4257.i4Utils.bytecode;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.io.TrackInputStream;
import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.ir.IRExc;
import illa4257.i4Utils.lists.Iter;
import illa4257.i4Utils.logger.i4Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
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
                final ArrayList<Exc> excs = new ArrayList<>();
                final ArrayList<Inst> instructions = m.instructions;
                final Stack<Integer> instSizes = new Stack<>();
                final ArrayList<IRAnchor> track = new ArrayList<>();
                final ArrayList<Stack<Object>> activeStacks = new ArrayList<>();
                final HashMap<IRAnchor, ArrayList<Stack<Object>>> stacks = new HashMap<>();
                final HashMap<Stack<Object>, Inst> stackStarters = new HashMap<>();
                activeStacks.add(new Stack<>());
                try (final TrackInputStream is = new TrackInputStream(new ByteArrayInputStream(attr.info))) {
                    IO.readBEShort(is); // Max Stack
                    IO.readBEShort(is); // Max Locals

                    final int totalCodeLen = IO.readBEInt(is);
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
                        exc.exc.anchor = new IRAnchor(exc);
                        excs.add(exc);
                    }
                    is.reset();
                    for (int i1 = 0, i2 = 0, ot = (m.access.contains(IRAccess.STATIC) ? 0 : 1), t = m.argumentsTypes.size() + ot; i1 < t; i1++, i2++) {
                        instructions.add(new Inst(Opcode.STORE, new IRRegister(i2), new Object[]{ new IRArg(i1) }));
                        instSizes.add(0);
                        if (i1 < ot)
                            continue;
                        final IRType type = m.argumentsTypes.get(i1 - ot);
                        if ((type.kind == IRType.Kind.LONG || type.kind == IRType.Kind.DOUBLE) && type.array == 0)
                            i2++;
                    }
                    final AtomicInteger instSize = new AtomicInteger(instructions.size()), counter = new AtomicInteger();
                    final Consumer<IRAnchor> newBranch = a -> {
                        if (!(a.id instanceof Long))
                            throw new RuntimeException("Not allowed " + a);
                        if ((long) a.id < 1)
                            return;
                        final ArrayList<Stack<Object>> vs = new ArrayList<>();
                        for (final Stack<Object> s : activeStacks) {
                            final Stack<Object> s2 = new Stack<>();
                            s2.addAll(s);
                            vs.add(s2);
                            stackStarters.put(s2, instructions.get(instructions.size() - 1));
                        }
                        stacks.put(a, vs);
                    };
                    final ArrayList<Object> bits64 = new ArrayList<>();
                    final BiConsumer<Object, Boolean> pushOperand = (v, is64bit) -> {
                        if (activeStacks.isEmpty())
                            throw new RuntimeException("Illegal state");
                        for (final Stack<Object> stack : activeStacks)
                            stack.add(v);
                        if (v == Const.STACK_1 || v == Const.STACK_2)
                            return;
                        if (is64bit && !bits64.contains(v))
                            bits64.add(v);
                    };
                    final Consumer<Boolean> push = is64Bit -> {
                        if (activeStacks.isEmpty())
                            throw new RuntimeException("Illegal state");
                        final Object v = is64Bit ? Const.STACK_2 : Const.STACK_1;
                        for (final Stack<Object> stack : activeStacks)
                            stack.add(v);
                    };
                    final Function<Object, Boolean> isNot64Bit = o -> !(bits64.contains(o) || o == Const.STACK_2);
                    final Supplier<Object> popOperand = () -> {
                        if (activeStacks.isEmpty())
                            throw new RuntimeException("Illegal state");
                        /*final ArrayList<Object> results = new ArrayList<>();
                        for (final Stack<Object> stack : activeStacks)
                            results.add(stack.pop());
                        boolean onlyInst = true;
                        for (final Object o : results)
                            if (!(o instanceof Inst))
                                onlyInst = false;
                        if (onlyInst) {
                            final IRTmp r = new IRTmp(counter.getAndIncrement());
                            for (final Object o : results)
                                if (o instanceof Inst)
                                    ((Inst) o).output = r;
                            return r;
                        }
                        if (results.size() == 1)
                            return results.get(0);
                        final IRTmp r = new IRTmp(counter.getAndIncrement());
                        for (final Object o : results) {
                            if (o instanceof Inst) {
                                ((Inst) o).output = r;
                                continue;
                            }
                            boolean f = true;
                            int i = 1;
                            for (final Inst inst : instructions)
                                if (inst.output == o) {
                                    f = false;
                                    break;
                                } else
                                    i++;
                            if (f)
                                throw new RuntimeException("Not found " + i);
                            instructions.add(i, new Inst(Opcode.STORE, r, new Object[] { o }));
                            instSizes.add(i, 0);
                            instSize.getAndIncrement();
                        }
                        return r;*/
                        final ArrayList<Object> results = new ArrayList<>();
                        for (final Stack<Object> stack : activeStacks)
                            results.add(stack.pop());
                        boolean isExc = false, isR = false;
                        for (final Object r : results)
                            if (r == Const.EXCEPTION)
                                isExc = true;
                            else
                                isR = true;
                        if (isR == isExc)
                            throw new RuntimeException("Illegal state");
                        if (isExc)
                            return Const.EXCEPTION;
                        return isNot64Bit.apply(results.get(0)) ? Const.STACK_1 : Const.STACK_2;
                    };
                    final ArrayList<IRAnchor> toBeRemoved = new ArrayList<>();
                    final Consumer<Long> loadStacks = l -> {
                        for (final Map.Entry<IRAnchor, ArrayList<Stack<Object>>> e : stacks.entrySet()) {
                            if (e.getKey().id instanceof Exc)
                                continue;
                            if (l != (long) e.getKey().id)
                                continue;
                            activeStacks.addAll(e.getValue());
                            toBeRemoved.add(e.getKey());
                        }
                        for (final IRAnchor a : toBeRemoved)
                            stacks.remove(a);
                        toBeRemoved.clear();
                    };
                    boolean add = true;
                    final long codeStart = is.position;
                    while (is.position - codeStart != totalCodeLen) {
                        final long instructionStart = is.position - codeStart;
                        final Function<Integer, IRAnchor> newAnchor = offset -> {
                            final long o = instructionStart + offset;
                            final IRAnchor anchor = new IRAnchor(o);
                            if (offset < 0) {
                                int i = 0;
                                long p = 0;
                                while (true) {
                                    if (p >= o) {
                                        final Inst old = instructions.get(i);
                                        if (old.opcode == Opcode.ANCHOR)
                                            anchor.id = old.params[0];
                                        else {
                                            instructions.add(i, new Inst(Opcode.ANCHOR, new Object[]{ o }));
                                            instSizes.add(i, 0);
                                            instSize.getAndIncrement();
                                        }
                                        break;
                                    }
                                    p += instSizes.get(i++);
                                }
                            } else
                                track.add(anchor);
                            return anchor;
                        };
                        {
                            IRAnchor f = null;
                            Iterator<IRAnchor> ai = track.iterator();
                            while (ai.hasNext()) {
                                final IRAnchor anchor = ai.next();
                                if ((long) anchor.id != instructionStart)
                                    continue;
                                if (f == null) {
                                    f = anchor;
                                    final Inst old = !instructions.isEmpty() ? instructions.get(instructions.size() - 1) : null;
                                    if (old.opcode == Opcode.ANCHOR)
                                        anchor.id = old.params[0];
                                    else {
                                        instructions.add(new Inst(Opcode.ANCHOR, new Object[]{anchor.id}));
                                        instSizes.add(0);
                                        instSize.getAndIncrement();
                                    }
                                    loadStacks.accept(instructionStart);
                                } else
                                    anchor.id = f.id;
                                ai.remove();
                            }
                            for (final Exc exc : Iter.reversible(excs))
                                if (exc.start == instructionStart) {
                                    instructions.add(new Inst(Opcode.TRY, new Object[]{ exc.exc }));
                                    instSizes.add(0);
                                    instSize.getAndIncrement();
                                }
                            for (final Exc exc : excs)
                                if (exc.end == instructionStart) {
                                    instructions.add(new Inst(Opcode.CATCH, new Object[] { exc.exc, exc.exc.anchor }));
                                    instSizes.add(0);
                                    instSize.getAndIncrement();
                                    final ArrayList<Stack<Object>> l = new ArrayList<>();
                                    l.add(new Stack<>());
                                    stacks.put(exc.exc.anchor, l);
                                }
                            for (final Exc exc : excs)
                                if (exc.offset == instructionStart) {
                                    final Inst old = !instructions.isEmpty() ? instructions.get(instructions.size() - 1) : null;
                                    if (old != null && old.opcode == Opcode.ANCHOR) {
                                        exc.exc.anchor.id = old.params[0];
                                    } else {
                                        instructions.add(new Inst(Opcode.ANCHOR, new Object[]{ exc.exc.anchor.id = instructionStart }));
                                        instSizes.add(0);
                                        instSize.getAndIncrement();
                                        loadStacks.accept(instructionStart);
                                    }
                                    activeStacks.clear();
                                    activeStacks.add(new Stack<>());
                                    pushOperand.accept(Const.EXCEPTION, false);
                                }
                        }
                        final int b = IO.readByteI(is);
                        switch (b) {
                            case 0: { // NOOP
                                instructions.add(new Inst(Opcode.NO_OP, 0));
                                break;
                            }
                            case 1: // null
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ null });
                                instructions.add(inst);
                                push.accept(false);
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
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRInt(b - 3) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 9: // lconst_0
                            case 10: // lconst_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_2, new Object[]{ new IRLong(b - 9) });
                                instructions.add(inst);
                                push.accept(true);
                                break;
                            }

                            case 11: // fconst_0
                            case 12: // fconst_1
                            case 13: // fconst_2
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRFloat(b - 11) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 14: // dconst_0
                            case 15: // dconst_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_2, new Object[]{ new IRDouble(b - 14) });
                                instructions.add(inst);
                                push.accept(true);
                                break;
                            }

                            case 16: // bipush
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRByte(IO.readByte(is)) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 17: // sipush
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRShort(IO.readBEShort(is)) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 18: // ldc
                            case 19: // ldc_w
                            case 20: // ldc2_w
                            {
                                Object v;
                                if (b == 18)
                                    v = cf.constantPool.get(IO.readByteI(is) - 1);
                                else
                                    v = cf.constantPool.get(IO.readBEShortI(is) - 1);

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
                                    v = cf.constantPool.get(((ClassFile.StrTag) v).stringIndex - 1);
                                else
                                    throw new RuntimeException("Unknown type of constants: " + v);

                                final boolean is64Bit = v instanceof IRLong || v instanceof IRDouble;
                                final Inst inst = new Inst(Opcode.STORE, is64Bit ? Const.STACK_2 : Const.STACK_1, new Object[]{ v });
                                instructions.add(inst);
                                push.accept(is64Bit);
                                break;
                            }

                            case 21: // iload
                            case 22: // lload
                            case 23: // fload
                            case 24: // dload
                            case 25: // aload
                            {
                                instructions.add(new Inst(Opcode.STORE, b == 22 || b == 24 ? Const.STACK_2 : Const.STACK_1, new Object[]{ new IRRegister(IO.readByteI(is)) }));
                                push.accept(b == 22 || b == 24);
                                break;
                            }

                            case 26: // iload_0
                            case 27: // iload_1
                            case 28: // iload_2
                            case 29: // iload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRRegister(b - 26) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 30: // lload_0
                            case 31: // lload_1
                            case 32: // lload_2
                            case 33: // lload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_2, new Object[]{ new IRRegister(b - 30) });
                                instructions.add(inst);
                                push.accept(true);
                                break;
                            }

                            case 34: // fload_0
                            case 35: // fload_1
                            case 36: // fload_2
                            case 37: // fload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRRegister(b - 34) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 38: // dload_0
                            case 39: // dload_1
                            case 40: // dload_2
                            case 41: // dload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_2, new Object[]{ new IRRegister(b - 38) });
                                instructions.add(inst);
                                push.accept(true);
                                break;
                            }

                            case 42: // aload_0
                            case 43: // aload_1
                            case 44: // aload_2
                            case 45: // aload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, Const.STACK_1, new Object[]{ new IRRegister(b - 42) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 46: // iaload
                            case 47: // laload
                            case 48: // faload
                            case 49: // daload
                            case 50: // aaload
                            case 51: // baload
                            case 52: // caload
                            case 53: // saload
                            {
                                final Object index = popOperand.get();
                                final Inst inst = new Inst(Opcode.ARRAY_GET, b == 47 || b == 49 ? Const.STACK_2 : Const.STACK_1, new Object[]{popOperand.get(), index});
                                instructions.add(inst);
                                push.accept(b == 47 || b == 49);
                                break;
                            }

                            case 54: // istore
                            case 55: // lstore
                            case 56: // fstore
                            case 57: // dstore
                            case 58: // astore
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(IO.readByteI(is)), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 59: // istore_0
                            case 60: // istore_1
                            case 61: // istore_2
                            case 62: // istore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 59), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 63: // lstore_0
                            case 64: // lstore_1
                            case 65: // lstore_2
                            case 66: // lstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 63), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 67: // fstore_0
                            case 68: // fstore_1
                            case 69: // fstore_2
                            case 70: // fstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 67), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 71: // dstore_0
                            case 72: // dstore_1
                            case 73: // dstore_2
                            case 74: // dstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 72), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 75: // astore_0
                            case 76: // astore_1
                            case 77: // astore_2
                            case 78: // astore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 75), new Object[]{ popOperand.get() }));
                                break;
                            }

                            case 79: // iastore
                            case 80: // lastore
                            case 81: // fastore
                            case 82: // dastore
                            case 83: // aastore
                            case 84: // bastore
                            case 85: // castore
                            case 86: // sastore
                            {
                                final Object val = popOperand.get(), index = popOperand.get();
                                instructions.add(new Inst(Opcode.ARRAY_SET, null, new Object[]{ popOperand.get(), index, val }));
                                break;
                            }

                            case 88: { // pop2
                                if (activeStacks.isEmpty())
                                    throw new RuntimeException("Illegal state");
                                for (final Stack<Object> stack : activeStacks)
                                    if (isNot64Bit.apply(stack.pop()))
                                        stack.pop();
                                instructions.add(new Inst(Opcode.POP2, 0));
                                break;
                            }
                            case 87: { // pop
                                if (activeStacks.isEmpty())
                                    throw new RuntimeException("Illegal state");
                                for (final Stack<Object> stack : activeStacks)
                                    stack.pop();
                                instructions.add(new Inst(Opcode.POP, 0));
                                break;
                            }

                            case 89: { // dup
                                Object val = popOperand.get();
                                pushOperand.accept(val, false);
                                pushOperand.accept(val, false);
                                instructions.add(new Inst(Opcode.DUP, 0));
                                break;
                            }

                            case 90: // dup_x1
                            {
                                final Object d = popOperand.get(), o = popOperand.get();
                                pushOperand.accept(d, false);
                                pushOperand.accept(o, false);
                                pushOperand.accept(d, false);
                                instructions.add(new Inst(Opcode.DUP_x1, 0));
                                break;
                            }

                            case 91: // dup_x2
                            {
                                final Object d = popOperand.get(), o1 = popOperand.get();
                                final boolean nb64 = isNot64Bit.apply(d);
                                final Object o2 = nb64 ? popOperand.get() : null;
                                pushOperand.accept(d, false);
                                pushOperand.accept(o1, !nb64);
                                if (nb64)
                                    pushOperand.accept(o2, false);
                                pushOperand.accept(d, false);
                                instructions.add(new Inst(Opcode.DUP_x2, 0));
                                break;
                            }

                            case 92: // dup2
                            {
                                final Object v1 = popOperand.get();
                                final boolean nb64 = isNot64Bit.apply(v1);
                                if (nb64) {
                                    final Object v2 = popOperand.get();
                                    pushOperand.accept(v2, false);
                                    pushOperand.accept(v1, false);
                                    pushOperand.accept(v2, false);
                                } else
                                    pushOperand.accept(v1, true);
                                pushOperand.accept(v1, !nb64);
                                instructions.add(new Inst(Opcode.DUP2, 0));
                                break;
                            }

                            case 93: // dup2_x1
                            {
                                final Object v1 = popOperand.get(), v2 = popOperand.get();
                                final boolean nb64 = isNot64Bit.apply(v1);
                                if (nb64) {
                                    final Object v3 = popOperand.get();
                                    pushOperand.accept(v2, false);
                                    pushOperand.accept(v1, false);
                                    pushOperand.accept(v3, false);
                                } else
                                    pushOperand.accept(v1, true);
                                pushOperand.accept(v2, !nb64);
                                pushOperand.accept(v1, !nb64);
                                instructions.add(new Inst(Opcode.DUP2_x1, 0));
                                break;
                            }

                            case 94: // dup2_x2
                            {
                                final Object v1 = popOperand.get(), v2 = popOperand.get();
                                final boolean nb64 = isNot64Bit.apply(v1), nb64_2 = isNot64Bit.apply(v2);
                                if (nb64) {
                                    if (nb64_2) {
                                        final Object v3 = popOperand.get();
                                        final boolean nb64_3 = isNot64Bit.apply(v3);
                                        if (nb64_3) {
                                            final Object v4 = popOperand.get();
                                            if (isNot64Bit.apply(v4)) { // Form 1
                                                pushOperand.accept(v2, false);
                                                pushOperand.accept(v1, false);
                                                pushOperand.accept(v4, false);
                                            } else
                                                throw new RuntimeException("Illegal state");
                                        } else { // Form 3
                                            pushOperand.accept(v2, false);
                                            pushOperand.accept(v1, false);
                                        }
                                        pushOperand.accept(v3, !nb64_3);
                                    } else
                                        throw new RuntimeException("Illegal state");
                                } else if (nb64_2) {
                                    final Object v3 = popOperand.get();
                                    final boolean nb64_3 = isNot64Bit.apply(v3);
                                    if (nb64_3) { // Form 2
                                        pushOperand.accept(v1, true);
                                        pushOperand.accept(v3, false);
                                    } else
                                        throw new RuntimeException("Illegal state");
                                } else // Form 4
                                    pushOperand.accept(v1, true);
                                pushOperand.accept(v2, !nb64_2);
                                pushOperand.accept(v1, !nb64);
                                instructions.add(new Inst(Opcode.DUP2_x2, 0));
                                break;
                            }

                            case 95: // swap
                            {
                                final Object v1 = popOperand.get(), v2 = popOperand.get();
                                pushOperand.accept(v1, !isNot64Bit.apply(v1));
                                pushOperand.accept(v2, !isNot64Bit.apply(v2));
                                instructions.add(new Inst(Opcode.SWAP, 0));
                                break;
                            }

                            case 96: // iadd
                            case 97: // ladd
                            case 98: // fadd
                            case 99: // dadd
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.ADD, b == 97 || b == 99 ? Const.STACK_2 : Const.STACK_1, new Object[]{ popOperand.get(), val2, nType(b - 96) });
                                instructions.add(inst);
                                push.accept(b == 97 || b == 99);
                                break;
                            }

                            case 100: // isub
                            case 101: // lsub
                            case 102: // fsub
                            case 103: // dsub
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.SUBTRACT, b == 101 || b == 103 ? Const.STACK_2 : Const.STACK_1, new Object[]{ popOperand.get(), val2, nType(b - 100) });
                                instructions.add(inst);
                                push.accept(b == 101 || b == 103);
                                break;
                            }

                            case 104: // imul
                            case 105: // lmul
                            case 106: // fmul
                            case 107: // dmul
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.MULTIPLY, b == 105 || b == 107 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), val2, nType(b - 104) });
                                instructions.add(inst);
                                push.accept(b == 105 || b == 107);
                                break;
                            }

                            case 108: // idiv
                            case 109: // ldiv
                            case 110: // fdiv
                            case 111: // ddiv
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.DIVIDE, b == 109 || b == 111 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), val2, nType(b - 108) });
                                instructions.add(inst);
                                push.accept(b == 109 || b == 111);
                                break;
                            }

                            case 112: // irem
                            case 113: // lrem
                            case 114: // frem
                            case 115: // drem
                            {
                                Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.REMAINDER, b == 113 || b == 115 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), val2, nType(b - 112) });
                                instructions.add(inst);
                                pushOperand.accept(inst, b == 113 || b == 115);
                                break;
                            }

                            case 116: // ineg
                            case 117: // lneg
                            case 118: // fneg
                            case 119: // dneg
                            {
                                final Inst inst = new Inst(Opcode.NEGATIVE, b == 117 || b == 119 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get() });
                                instructions.add(inst);
                                pushOperand.accept(inst, b == 117 || b == 119);
                                break;
                            }

                            case 120: // ishl
                            case 121: // lshl
                            {
                                final Object s = popOperand.get();
                                final Inst inst = new Inst(Opcode.SHIFT_LEFT, b == 121 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), s, nType(b - 120) });
                                instructions.add(inst);
                                pushOperand.accept(inst, b == 121);
                                break;
                            }

                            case 122: // ishr
                            case 123: // lshr
                            {
                                final Object s = popOperand.get();
                                final Inst inst = new Inst(Opcode.SHIFT_RIGHT, b == 123 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), s, nType(b - 122) });
                                instructions.add(inst);
                                pushOperand.accept(inst, b == 123);
                                break;
                            }

                            case 124: // iushr
                            case 125: // lushr
                            {
                                final Object s = popOperand.get();
                                final Inst inst = new Inst(Opcode.UNSIGNED_SHIFT_RIGHT, b == 125 ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), s, nType(b - 124) });
                                instructions.add(inst);
                                pushOperand.accept(inst, b == 125);
                                break;
                            }

                            case 126: // iand
                            case 127: // land
                            case 128: // ior
                            case 129: // lor
                            case 130: // ixor
                            case 131: // lxor
                            {
                                final Object v2 = popOperand.get();
                                final boolean isLong = b == 127 || b== 129 || b == 131;
                                final Inst inst = new Inst(b == 126 || b == 127 ? Opcode.AND : b == 128 || b == 129 ? Opcode.OR : Opcode.XOR, isLong ? Const.STACK_2 : Const.STACK_1, new Object[] { popOperand.get(), v2,
                                        isLong ? IRType.Kind.LONG : IRType.Kind.INT });
                                instructions.add(inst);
                                pushOperand.accept(inst, isLong);
                                break;
                            }

                            case 132: // iinc
                            {
                                final int i = IO.readByteI(is);
                                instructions.add(new Inst(Opcode.ADD, new IRRegister(i), new Object[] { new IRRegister(i), new IRInt(IO.readByte(is)), IRType.Kind.INT }));
                                break;
                            }

                            case 133: // i2l
                            case 134: // i2f
                            case 135: // i2d
                            case 136: // l2i
                            case 137: // l2f
                            case 138: // l2d
                            case 139: // f2i
                            case 140: // f2l
                            case 141: // f2d
                            case 142: // d2i
                            case 143: // d2l
                            case 144: // d2f
                            case 145: // i2b
                            case 146: // i2c
                            case 147: // i2s
                            {
                                final boolean isLong = b == 133 || b == 135 || b == 138 || b == 140 || b == 141 || b == 144;
                                final Inst inst = new Inst(
                                        b == 133 ? Opcode.INT2LONG :
                                        b == 134 ? Opcode.INT2FLOAT :
                                        b == 135 ? Opcode.INT2DOUBLE :

                                        b == 136 ? Opcode.LONG2INT :
                                        b == 137 ? Opcode.LONG2FLOAT :
                                        b == 138 ? Opcode.LONG2DOUBLE :

                                        b == 139 ? Opcode.FLOAT2INT :
                                        b == 140 ? Opcode.FLOAT2LONG :
                                        b == 141 ? Opcode.FLOAT2DOUBLE :

                                        b == 142 ? Opcode.DOUBLE2INT :
                                        b == 143 ? Opcode.DOUBLE2FLOAT :
                                        b == 144 ? Opcode.DOUBLE2LONG :

                                        b == 145 ? Opcode.INT2BYTE :
                                        b == 146 ? Opcode.INT2CHAR :
                                                Opcode.INT2SHORT,
                                        isLong ? Const.STACK_2 : Const.STACK_1, new Object[]{ popOperand.get() });
                                instructions.add(inst);
                                push.accept(isLong);
                                break;
                            }

                            case 148: // lcmp
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.COMPARE, Const.STACK_1, new Object[]{popOperand.get(), val2});
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 149: // fcmpl
                            case 150: // fcmpg
                            case 151: // dcmpl
                            case 152: // dcmpg
                            {
                                final Object val2 = popOperand.get();
                                final Inst inst = new Inst(Opcode.COMPARE_NAN, Const.STACK_1, new Object[]{popOperand.get(), val2, b == 149 || b == 151 ? new IRInt(-1) : new IRInt(1)});
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 153: // if_eq
                            case 154: // if_ne
                            case 155: // if_lt
                            case 156: // if_ge
                            case 157: // if_gt
                            case 158: // if_le
                            {
                                final Object v = popOperand.get();
                                final IRAnchor anchor = newAnchor.apply((int) IO.readBEShort(is));
                                instructions.add(new Inst(
                                        b == 153 ? Opcode.IF_EQ :
                                        b == 154 ? Opcode.IF_NE :
                                        b == 155 ? Opcode.IF_LT :
                                        b == 156 ? Opcode.IF_GE :
                                        b == 157 ? Opcode.IF_GT :
                                                Opcode.IF_LE
                                        , new Object[] { v, new IRInt(0), anchor }));
                                newBranch.accept(anchor);
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
                                Object val2 = popOperand.get(), val1 = popOperand.get();
                                final IRAnchor anchor = newAnchor.apply((int) IO.readBEShort(is));
                                instructions.add(new Inst(
                                        b == 159 || b == 165 ? Opcode.IF_EQ :
                                                b == 160 || b == 166 ? Opcode.IF_NE :
                                                        b == 161 ? Opcode.IF_LT :
                                                                b == 162 ? Opcode.IF_GE :
                                                                        b == 163 ? Opcode.IF_GT :
                                                                                Opcode.IF_LE
                                        , new Object[] { val1, val2, anchor }));
                                newBranch.accept(anchor);
                                break;
                            }

                            case 167: { // goto
                                final IRAnchor anchor = newAnchor.apply((int) IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { anchor }));
                                newBranch.accept(anchor);
                                activeStacks.clear();
                                break;
                            }

                            case 168: // jsr
                            { // TODO: Check it with java -5
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { newAnchor.apply((int) IO.readBEShort(is)) }));
                                // TODO: Check the operand stack.
                                break;
                            }

                            case 169: // ret
                            {
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { new IRAnchor(new IRRegister(IO.readByteI(is))) }));
                                break;
                            }

                            case 170: // tableswitch
                            {
                                // TODO: Implement it!
                                throw new RuntimeException("Not implemented");
                            }

                            case 171: // lookupswitch
                            {
                                // TODO: Implement it!
                                throw new RuntimeException("Not implemented");
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
                                instructions.add(new Inst(Opcode.RETURN, 0));
                                activeStacks.clear();
                                break;

                            case 178: { // getstatic
                                final IRFieldRef ref = fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1));
                                final boolean isLong = ref.type.kind == IRType.Kind.LONG || ref.type.kind == IRType.Kind.DOUBLE;
                                final Inst inst = new Inst(Opcode.GET_STATIC, isLong ? Const.STACK_2 : Const.STACK_1, new Object[] { ref });
                                instructions.add(inst);
                                push.accept(isLong);
                                break;
                            }

                            case 179: { // putstatic
                                instructions.add(new Inst(Opcode.PUT_STATIC, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)), popOperand.get() }));
                                break;
                            }

                            case 180: // getfield
                            {
                                final IRFieldRef ref = fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1));
                                final boolean isLong = ref.type.kind == IRType.Kind.LONG || ref.type.kind == IRType.Kind.DOUBLE;
                                final Inst inst = new Inst(Opcode.GET_FIELD, isLong ? Const.STACK_2 : Const.STACK_1, new Object[] { ref, popOperand.get() });
                                instructions.add(inst);
                                push.accept(isLong);
                                break;
                            }

                            case 181: // putfield
                            {
                                Object val = popOperand.get();
                                instructions.add(new Inst(Opcode.PUT_FIELD, new Object[] { fieldRef(cf, (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1)), popOperand.get(), val }));
                                break;
                            }

                            case 182: // invokevirtual
                            case 183: // invokespecial
                            case 184: // invokestatic
                            {
                                final ClassFile.MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                final IRMethodRef mr = methodRef(cf, ref);
                                final int l = d.parameters.size() + (b == 184 ? 1 : 2);
                                final Inst inst = new Inst(b == 182 ? Opcode.INVOKE_VIRTUAL : b == 183 ? Opcode.INVOKE_SPECIAL : Opcode.INVOKE_STATIC, l);
                                inst.params[0] = mr;
                                for (int i = l - 1; i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID) {
                                    final boolean isLong = mr.type.kind == IRType.Kind.LONG || mr.type.kind == IRType.Kind.DOUBLE;
                                    inst.output = isLong ? Const.STACK_2 : Const.STACK_1;
                                    push.accept(isLong);
                                }
                                break;
                            }

                            case 185: // invokeinterface
                            {
                                final InterfaceMethodRef ref = (ClassFile.InterfaceMethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                final IRMethodRef mr = methodRef(cf, ref);
                                IO.readByteI(is); // stack size
                                if (IO.readByte(is) != 0)
                                    throw new RuntimeException("Not valid the invoke interface instruction.");
                                final Inst inst = new Inst(Opcode.INVOKE_INTERFACE, d.parameters.size() + 2);
                                inst.params[0] = mr;
                                for (int i = d.parameters.size() + 1; i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID) {
                                    final boolean isLong = mr.type.kind == IRType.Kind.LONG || mr.type.kind == IRType.Kind.DOUBLE;
                                    inst.output = isLong ? Const.STACK_2 : Const.STACK_1;
                                    push.accept(isLong);
                                }
                                break;
                            }

                            case 186: // invokedynamic
                            {
                                final MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShortI(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                final IRMethodRef mr = methodRef(cf, ref);
                                if (IO.readByte(is) != 0 || IO.readByte(is) != 0)
                                    throw new RuntimeException("Not valid the invoke interface instruction.");
                                final Inst inst = new Inst(Opcode.INVOKE_DYNAMIC, d.parameters.size() + 2);
                                inst.params[0] = mr;
                                for (int i = d.parameters.size(); i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID) {
                                    final boolean isLong = mr.type.kind == IRType.Kind.LONG || mr.type.kind == IRType.Kind.DOUBLE;
                                    inst.output = isLong ? Const.STACK_2 : Const.STACK_1;
                                    push.accept(isLong);
                                }
                                break;
                            }

                            case 187: { // new
                                final Inst inst = new Inst(Opcode.ALLOCATE, Const.STACK_1, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 188: { // newarray
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
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, Const.STACK_1, new Object[] { kind, popOperand.get() });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 189: { // anewarray
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, Const.STACK_1, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShortI(is) - 1)).nameIndex - 1), popOperand.get() });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 190: { // arraylength
                                final Inst inst = new Inst(Opcode.ARRAY_LENGTH, Const.STACK_1, new Object[]{ popOperand.get() });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 191: { // athrow
                                instructions.add(new Inst(Opcode.THROW, new Object[]{ popOperand.get() }));
                                activeStacks.clear();
                                break;
                            }

                            case 192: { // checkcast
                                final Object operand = popOperand.get();
                                pushOperand.accept(operand, !isNot64Bit.apply(operand));
                                instructions.add(new Inst(Opcode.CHECK_CAST, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) }));
                                break;
                            }

                            case 193: { // instanceof
                                final Object operand = popOperand.get();
                                final Inst inst = new Inst(Opcode.INSTANCEOF, Const.STACK_1, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 194: // monitorenter
                            case 195: // monitorexit
                            {
                                instructions.add(new Inst(b == 194 ? Opcode.MONITOR_ENTER : Opcode.MONITOR_EXIT, new Object[] { popOperand.get() }));
                                break;
                            }

                            case 196: // wide
                            {
                                final int b2 = IO.readByteI(is);
                                switch (b2) {
                                    case 21: // iload
                                    case 22: // lload
                                    case 23: // fload
                                    case 24: // dload
                                    case 25: // aload
                                    {
                                        final Inst inst = new Inst(Opcode.STORE, b2 == 22 || b2 == 24 ? Const.STACK_2 : Const.STACK_1, new Object[]{ new IRRegister(IO.readBEShortI(is)) });
                                        instructions.add(inst);
                                        push.accept(b2 == 22 || b2 == 24);
                                        break;
                                    }
                                    case 54: // istore
                                    case 55: // lstore
                                    case 56: // fstore
                                    case 57: // dstore
                                    case 58: // astore
                                    {
                                        instructions.add(new Inst(Opcode.STORE, new IRRegister(IO.readBEShortI(is)), new Object[]{ popOperand.get() }));
                                        break;
                                    }
                                    case 132: // iinc
                                    {
                                        final int i = IO.readBEShortI(is);
                                        instructions.add(new Inst(Opcode.ADD, new IRRegister(i), new Object[] { new IRRegister(i), new IRInt(IO.readBEShortI(is)), IRType.Kind.INT }));
                                        break;
                                    }
                                    default: throw new RuntimeException("Unknown wide opcode " + b);
                                }
                                break;
                            }

                            case 197: // multianewarray
                            {
                                final Object cls = cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShortI(is) - 1)).nameIndex - 1);
                                int dim = IO.readByteI(is);
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, Const.STACK_1, new Object[dim + 1]);
                                inst.params[0] = cls;
                                for (int i = dim; i > 0; i--)
                                    inst.params[i] = popOperand.get();
                                instructions.add(inst);
                                push.accept(false);
                                break;
                            }

                            case 198: // if_null
                            case 199: // if_nonnull
                            {
                                final Object v = popOperand.get();
                                final IRAnchor anchor = newAnchor.apply((int) IO.readBEShort(is));
                                instructions.add(new Inst(b == 198 ? Opcode.IF_NULL : Opcode.IF_NONNULL, new Object[] { v, anchor }));
                                newBranch.accept(anchor);
                                break;
                            }

                            case 200: { // goto_w
                                final IRAnchor anchor = newAnchor.apply((int) IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { anchor }));
                                newBranch.accept(anchor);
                                activeStacks.clear();
                                break;
                            }

                            case 201: // jsr_w
                            { // TODO: Check it with java -5
                                instructions.add(new Inst(Opcode.GOTO, new Object[] { newAnchor.apply((int) IO.readBEShort(is)) }));
                                // TODO: Check the operand stack.
                                break;
                            }

                            default:
                                throw new RuntimeException("Unknown operation: " + b);
                        }

                        {
                            final int instDelta = instructions.size() - instSize.get();
                            final long len = is.position - instructionStart - codeStart;
                            if (instDelta > 1)
                                throw new RuntimeException("Instruction has more than one instruction " + instDelta + " at position " + instructionStart);
                            if (add)
                                instSizes.add((int) len);
                            else
                                instSizes.add(instSizes.pop() + (int) len);
                            instSize.addAndGet(instDelta);
                            add = instDelta > 0;
                        }
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
                    /*if (m.name.equals("registerFilter")) {
                        int i = 0, p = 0;
                        System.out.println("[");
                        for (final Inst inst : instructions) {
                            final int l = instSizes.size() > i ? instSizes.get(i) : -1;
                            System.out.println("\t" + i + "\t" + p + "\t" + l + "\t" + inst.opcode + ' ' + Arrays.toString(inst.params) + " >> " + inst.output);
                            if (l == -1) {
                                p = -1;
                                continue;
                            }
                            p += l;
                            i++;
                        }
                        System.out.println("]");
                    }*/
                    IROperandStack.resolve(instructions);
                    //if (m.name.equals("registerFilter")) throw new RuntimeException("test");
                } catch (final RuntimeException e) {
                    i4Logger.INSTANCE.e("#" + m.name);
                    //m.print();
                    int i = 0, p = 0;
                    System.out.println("[");
                    for (final Inst inst : instructions) {
                        final int l = instSizes.size() > i ? instSizes.get(i) : -1;
                        System.out.println("\t" + i + "\t" + p + "\t" + l + "\t" + inst.opcode + ' ' + Arrays.toString(inst.params) + " >> " + inst.output);
                        if (l == -1) {
                            p = -1;
                            continue;
                        }
                        p += l;
                        i++;
                    }
                    System.out.println("]");
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
            ir.type = d.type.toIRType();
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
            try {
                cls.methods.add(m.toIRMethod(this));
            } catch (final RuntimeException e) {
                System.out.println(cls.name);
                throw e;
            }
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