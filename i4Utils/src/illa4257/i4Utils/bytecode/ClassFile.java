package illa4257.i4Utils.bytecode;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.ir.IRExc;
import illa4257.i4Utils.lists.Iter;
import illa4257.i4Utils.str.Str;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class ClassFile {
    public static final int MAGIC = 0xCAFEBABE;


    public static class IntTag {
        public int n;

        public IntTag(final int n) { this.n = n; }
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

    public static class InterfaceMethodRef extends Ref {
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
                final ArrayList<Inst> last = new ArrayList<>();
                final ArrayList<IRAnchor> track = new ArrayList<>();
                final ArrayList<Integer> steps = new ArrayList<>();//, catchOffsets = new ArrayList<>();
                final ArrayList<Exc> excs = new ArrayList<>();
                final ArrayList<Inst> instructions = m.instructions;
                final ArrayList<Object> operands = new ArrayList<>();
                try (final ByteArrayInputStream is = new ByteArrayInputStream(attr.info)) {
                    IO.readBEShort(is); // Max Stack
                    IO.readBEShort(is); // Max Locals

                    int old;
                    final int totalCodeLen;
                    int codeLen = old = totalCodeLen = IO.readBEInt(is), deltaBonus = 0, oldInstSize = 0;
                    is.mark(attr.info.length);
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
                    for (int i = 0, t = m.argumentsTypes.size() + (m.access.contains(IRAccess.STATIC) ? 0 : 1); i < t; i++) {
                        instructions.add(new Inst(Opcode.STORE, new IRRegister(i), new Object[]{ new IRParameter(i) }));
                        steps.add(0, 0);
                        oldInstSize++;
                    }
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
                                operands.clear();
                                operands.add(Const.EXCEPTION);
                            }
                        final int b = is.read();
                        switch (b) {
                            case 0: { // NOOP
                                instructions.add(new Inst(Opcode.NO_OP, 0));
                                break;
                            }
                            case 1: // null
                            {
                                operands.add(null);
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
                                operands.add(new IRInt(b - 3));
                                break;
                            }

                            case 9: // lconst_0
                            case 10: // lconst_1
                            {
                                operands.add(new IRLong(b - 9));
                                break;
                            }

                            case 18:
                                codeLen--;
                                final Object v = cf.constantPool.get(IO.readByteI(is) - 1);
                                if (v instanceof ClsTag) {
                                    operands.add(cf.constantPool.get(((ClassFile.StrTag) v).stringIndex - 1));
                                    break;
                                }
                                if (v instanceof IntTag) {
                                    operands.add(new IRInt(((IntTag) v).n));
                                    break;
                                }
                                if (v instanceof StrTag) {
                                    operands.add(cf.constantPool.get(((ClassFile.StrTag) v).stringIndex - 1));
                                    break;
                                }
                                throw new RuntimeException("Unknown type of constants: " + v);

                            case 21: // iload
                            case 22: // lload
                            case 25: // aload
                            {
                                codeLen--;
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(IO.readByteI(is))});
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 26: // iload_0
                            case 27: // iload_1
                            case 28: // iload_2
                            case 29: // iload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[] { new IRRegister(b - 26) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }
                            
                            case 30: // lload_0
                            case 31: // lload_1
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[] { new IRRegister(b - 30) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.LONG_RESULT);
                                break;
                            }

                            case 38: // fload_0
                            case 39: // fload_1
                            case 40: // fload_2
                            case 41: // fload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{ new IRRegister(b - 38) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 42: // aload_0
                            case 43: // aload_1
                            case 44: // aload_2
                            case 45: // aload_3
                            {
                                final Inst inst = new Inst(Opcode.STORE, new Object[]{new IRRegister(b - 42)});
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 50: { // aaload
                                final Object index = popOperand(operands, last);
                                final Object array = popOperand(operands, last);

                                final Inst inst = new Inst(Opcode.ARRAY_GET, new Object[] { array, index });

                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 54: // istore
                            case 55: // lstore
                            case 57: // dstore
                            case 58: // astore
                            {
                                codeLen--;
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(IO.readByteI(is)), new Object[] { popOperand(operands, last) }));
                                break;
                            }

                            case 59:
                            case 60:
                            case 61:
                            case 62:
                            { // istore_x
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 59), new Object[] { popOperand(operands, last) }));
                                break;
                            }

                            case 63: // lstore_0
                            case 64: // lstore_1
                            case 65: // lstore_2
                            case 66: // lstore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 63), new Object[] { popOperand(operands, last) }));
                                break;
                            }

                            case 75: // astore_0
                            case 76: // astore_1
                            case 77: // astore_2
                            case 78: // astore_3
                            {
                                instructions.add(new Inst(Opcode.STORE, new IRRegister(b - 75), new Object[] { popOperand(operands, last) }));
                                break;
                            }

                            case 88: { // pop2
                                final Object r = operands.remove(operands.size() - 1);
                                if (r == Const.LONG_RESULT) {
                                    last.remove(last.size() - 1);
                                    break;
                                }
                                if (r == Const.RESULT) {
                                    final Inst inst = last.remove(last.size() - 1);
                                    final IRMethodRef ref = (IRMethodRef) inst.params[0];
                                    if ((ref.type.kind == IRType.Kind.LONG || ref.type.kind == IRType.Kind.DOUBLE) && ref.type.array == 0)
                                        break;
                                }
                            }
                            case 87: { // pop
                                final Object r = operands.remove(operands.size() - 1);
                                if (r == Const.LONG_RESULT) {
                                    operands.add(Const.RESULT);
                                    break;
                                }
                                if (r == Const.RESULT)
                                    last.remove(last.size() - 1);
                                break;
                            }

                            case 89: { // dup
                                Object val = popOperand(operands, last);
                                operands.add(val);
                                operands.add(val);
                                break;
                            }

                            case 97: {
                                final Object val2 = popOperand(operands, last);
                                final Inst inst = new Inst(Opcode.ADD, new Object[] { popOperand(operands, last), val2 });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(inst);
                                break;
                            }

                            case 100: //isub
                            case 101: //lsub
                            {
                                Object val2 = popOperand(operands, last), val1 = popOperand(operands, last);
                                final Inst inst = new Inst(Opcode.SUBTRACT, new Object[] { val1, val2 });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 132: { // iinc
                                codeLen -= 2;
                                final int i = IO.readByteI(is);
                                instructions.add(new Inst(Opcode.ADD, new IRRegister(i), new Object[] { new IRRegister(i), new IRInt(IO.readByteI(is)) }));
                                break;
                            }

                            case 148: // lcmp
                            {
                                final Object val2 = popOperand(operands, last), val1 = popOperand(operands, last);
                                final Inst inst = new Inst(Opcode.COMPARE, new Object[]{ val1, val2 });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 153: { // if_eq
                                codeLen -= 2;
                                final IRAnchor bn = new IRAnchor(IO.readBEShort(is));
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_EQ, new Object[] { val, new IRInt(0), bn }));
                                track.add(bn);
                                break;
                            }

                            case 155: { // if_lt
                                codeLen -= 2;
                                final IRAnchor bn = new IRAnchor(IO.readBEShort(is));
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_LT, new Object[] { val, new IRInt(0), bn }));
                                track.add(bn);
                                break;
                            }

                            case 156: { // if_ge
                                codeLen -= 2;
                                final IRAnchor bn = new IRAnchor(IO.readBEShort(is));
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_GE, new Object[] { val, new IRInt(0), bn }));
                                track.add(bn);
                                break;
                            }

                            case 158: { // if_le
                                codeLen -= 2;
                                final IRAnchor bn = new IRAnchor(IO.readBEShort(is));
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_LE, new Object[] { val, new IRInt(0), bn }));
                                track.add(bn);
                                break;
                            }

                            case 162: { // if_icmpge
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                Object val2 = popOperand(operands, last), val1 = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_GE, new Object[] { val1, val2, skip }));
                                track.add(skip);
                                break;
                            }

                            case 164: { // if_icmple
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                Object val2 = popOperand(operands, last), val1 = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_LE, new Object[] { val1, val2, skip }));
                                track.add(skip);
                                break;
                            }

                            case 166: { // if_acmpne
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                Object val2 = popOperand(operands, last), val1 = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.IF_NE, new Object[] { val1, val2, skip }));
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

                            case 172: // ireturn
                            case 173: // lreturn
                            case 174: // freturn
                            case 175: // dreturn
                            case 176: // areturn
                            {
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.RETURN, new Object[]{ val }));
                                break;
                            }

                            case 177: // return
                                instructions.add(new Inst(Opcode.RETURN, 0));
                                break;

                            case 178: { // getstatic
                                codeLen -= 2;
                                final ClassFile.FieldRef ref = (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                //final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                //ps.println(cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(ref.classIndex - 1)).nameIndex - 1));
                                //ps.println(cf.constantPool.get(nt.nameIndex - 1));
                                //ps.println(cf.constantPool.get(nt.descriptorIndex - 1));

                                final Inst inst = new Inst(Opcode.GET_STATIC, new Object[] { fieldRef(cf, ref) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 179: {
                                codeLen -= 2; // putstatic
                                final ClassFile.FieldRef ref = (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                //final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);
                                //ps.println(cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(ref.classIndex - 1)).nameIndex - 1));
                                //ps.println(cf.constantPool.get(nt.nameIndex - 1));
                                //ps.println(cf.constantPool.get(nt.descriptorIndex - 1));

                                Object val = popOperand(operands, last);

                                instructions.add(new Inst(Opcode.PUT_STATIC, new Object[] { fieldRef(cf, ref), val }));
                                break;
                            }

                            case 180: // getfield
                            {
                                codeLen -= 2;
                                final ClassFile.FieldRef ref = (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                final Inst inst = new Inst(Opcode.GET_FIELD, new Object[] { fieldRef(cf, ref), popOperand(operands, last) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 181: {
                                codeLen -= 2; // putfield
                                final ClassFile.FieldRef ref = (ClassFile.FieldRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                Object val = popOperand(operands, last);
                                instructions.add(new Inst(Opcode.PUT_FIELD, new Object[] { fieldRef(cf, ref), popOperand(operands, last), val }));
                                break;
                            }

                            case 182: // invokevirtual
                            case 183: // invokespecial
                            case 184: // invokestatic
                            {
                                codeLen -= 2;
                                final ClassFile.MethodRef ref = (ClassFile.MethodRef) cf.constantPool.get(IO.readBEShort(is) - 1);
                                final ClassFile.NameAndType nt = (ClassFile.NameAndType) cf.constantPool.get(ref.nameAndTypeIndex - 1);

                                //ps.println(cf.constantPool.get(nt.nameIndex - 1));
                                //ps.println(cf.constantPool.get(nt.descriptorIndex - 1));

                                final Descriptor d = new Descriptor((String) cf.constantPool.get(nt.descriptorIndex - 1));
                                final int l = d.parameters.size() + (b == 184 ? 1 : 2);
                                final Inst inst = new Inst(b == 182 ? Opcode.INVOKE_VIRTUAL : b == 183 ? Opcode.INVOKE_SPECIAL : Opcode.INVOKE_STATIC, l);
                                inst.params[0] = methodRef(cf, ref);
                                for (int i = l - 1; i > 0; i--)
                                    inst.params[i] = popOperand(operands, last);
                                instructions.add(inst);
                                if (d.type != Descriptor.Type.VOID) {
                                    operands.add(Const.RESULT);
                                    last.add(inst);
                                }
                                break;
                            }

                            case 187: { // new
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.ALLOCATE, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 189: { // anewarray
                                codeLen -= 2;
                                final Inst inst = new Inst(Opcode.NEW_ARRAY, new Object[] { cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShortI(is) - 1)).nameIndex - 1), popOperand(operands, last) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 190: { // arraylength
                                final Inst inst = new Inst(Opcode.ARRAY_LENGTH, new Object[]{ popOperand(operands, last) });
                                instructions.add(inst);
                                operands.add(Const.RESULT);
                                last.add(inst);
                                break;
                            }

                            case 191: { // athrow
                                instructions.add(new Inst(Opcode.THROW, new Object[]{ popOperand(operands, last) }));
                                operands.clear();
                                break;
                            }

                            case 192: { // checkcast
                                codeLen -= 2;
                                final Object operand = popOperand(operands, last);
                                operands.add(operand);
                                instructions.add(new Inst(Opcode.CHECK_CAST, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) }));
                                break;
                            }

                            case 193: { // instanceof
                                codeLen -= 2;
                                final Object operand = popOperand(operands, last);
                                final Inst inst = new Inst(Opcode.INSTANCEOF, new Object[]{ operand, cf.constantPool.get(((ClassFile.ClsTag) cf.constantPool.get(IO.readBEShort(is) - 1)).nameIndex - 1) });
                                instructions.add(inst);
                                last.add(inst);
                                operands.add(Const.RESULT);
                                break;
                            }

                            case 194: // monitorenter
                            case 195: // monitorexit
                            {
                                instructions.add(new Inst(b == 194 ? Opcode.MONITOR_ENTER : Opcode.MONITOR_EXIT, new Object[] { popOperand(operands, last) }));
                                break;
                            }

                            case 198: { // if_null
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.IF_NULL, new Object[] { popOperand(operands, last), skip }));
                                track.add(skip);
                                break;
                            }

                            case 199: { // if_nonnull
                                codeLen -= 2;
                                final IRAnchor skip = new IRAnchor(IO.readBEShort(is));
                                instructions.add(new Inst(Opcode.IF_NONNULL, new Object[] { popOperand(operands, last), skip }));
                                track.add(skip);
                                break;
                            }

                            default:
                                throw new RuntimeException("Unknown operation: " + b);
                        }

                        final int delta = old - codeLen;
                        old = codeLen;
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
                                        n.id = instructions.size()-2-i;
                                        instructions.add(n.id, new Inst(Opcode.ANCHOR, new Object[] { n.id }));
                                        steps.add(steps.size() - n.id, 0);
                                        oldInstSize++;
                                        continue m;
                                    } else if (n.id > 0)
                                        throw new RuntimeException("Too much");
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
                                instructions.add(new Inst(Opcode.ANCHOR, new Object[] { n.id }));
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
                    if (!track.isEmpty())
                        throw new RuntimeException("Some tracks left: " + track);
                }
            }
            return m;
        }

        public static Object popOperand(final ArrayList<Object> operands, final ArrayList<Inst> last) {
            Object val = operands.remove(operands.size() - 1);
            if (val == Const.RESULT || val == Const.LONG_RESULT) {
                final Inst inst = last.remove(last.size() - 1);
                if (inst.output == null)
                    inst.output = new IRTmp(operands.size());
                val = inst.output;
            }
            return val;
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

        if (IO.readBEShort(inputStream) != 0) // Interfaces
            throw new RuntimeException("Not supported!");

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