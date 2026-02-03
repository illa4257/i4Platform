package illa4257.i4test;

import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.lists.ArrIterator;
import illa4257.i4Utils.ir.IRExc;
import illa4257.i4Utils.str.Str;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

public class IR2JS {
    public interface W {
        W w(final String str);
        default W t(final int n) { return this; }
        default W st(final int nt) { return this; }
        default W ln() { return this; }
    }

    public static class MW implements W {
        public final W w;

        public MW(final PrintStream ps) { this.w = new PSW(ps); }
        public MW(final W w) { this.w = w; }

        @Override
        public W w(final String str) {
            w.w(str);
            return this;
        }
    }

    public static class PSW implements W {
        public final PrintStream ps;
        public int nt = 0;

        public PSW(final PrintStream ps) { this.ps = ps; }

        @Override
        public W st(int nt) {
            this.nt = nt;
            return this;
        }

        @Override
        public W t(int n) {
            ps.print(Str.repeat("\t", n));
            return this;
        }

        @Override
        public W w(final String str) {
            ps.print(str);
            return this;
        }

        @Override
        public W ln() {
            ps.println();
            if (nt > 0)
                t(nt);
            return this;
        }
    }

    public static class SW implements W {
        public final W w;
        public final int o;
        
        public SW(final W w, final int o) { this.w = w; this.o = o; }
        
        @Override
        public W w(final String str) {
            w.w(str);
            return this;
        }

        @Override
        public W t(int n) {
            w.t(n);
            return this;
        }

        @Override
        public W st(int nt) {
            w.st(o + nt);
            return this;
        }

        @Override
        public W ln() {
            w.ln();
            return this;
        }
    }

    public static StringBuilder encType(final StringBuilder b, final IRType type) {
        switch (type.kind) {
            case VOID: b.append("void");break;
            case BOOLEAN: b.append("boolean");break;
            case BYTE: b.append("byte");break;
            case SHORT: b.append("short");break;
            case CHAR: b.append("char");break;
            case INT: b.append("int");break;
            case LONG: b.append("long");break;
            case FLOAT: b.append("float");break;
            case DOUBLE: b.append("double");break;
            case LITERAL: b.append(type.cls);break;
            default: throw new RuntimeException("Unknown kind: " + type.kind);
        }
        return Str.repeat(b, "[", type.array);
    }

    public static String encType(final IRType type) {
        final StringBuilder b = Str.builder();
        try {
            return encType(b, type).toString();
        } finally {
            Str.recycle(b);
        }
    }

    public static String methodName(final IRMethod method) {
        final StringBuilder b = Str.builder();
        try {
            b.append('"').append(method.name).append('(');
            Str.join(b, ", ", method.argumentsTypes, (t, sb) -> encType(sb, t));
            return encType(b.append(')'), method.type).append('"').toString();
        } finally {
            Str.recycle(b);
        }
    }

    public static String methodName(final IRMethodRef method) {
        final StringBuilder b = Str.builder();
        try {
            b.append('"').append(method.name).append('(');
            Str.join(b, ", ", method.params, (t, sb) -> encType(sb, t));
            return encType(b.append(')'), method.type).append('"').toString();
        } finally {
            Str.recycle(b);
        }
    }

    public static void write(final W o, final IRClass cls) {
        o.w("{").st(1);
        o.ln().w("name:\"" + cls.name + "\",");
        if (cls.superName != null)
            o.ln().w("super_cls:\"" + cls.superName + "\",");
        o.ln().w("declared_fields:{").st(2);
        for (final IRField f : cls.fields) {
            o.ln().w("\"_" + f.name + "\":\"").w(encType(f.type)).w("\",");
        }
        o.st(1).ln().w("},");
        for (final IRMethod m : cls.methods) {
            o.ln().w(methodName(m) + ":async function(");
            int i = 0;
            o.w("sc,env,t");
            if (!m.access.contains(IRAccess.STATIC)) {
                o.w(",p" + i);
                i++;
            }
            for (final IRType ignored : m.argumentsTypes)
                o.w(",p" + i++);
            o.w("){").st(2);
            if (m.access.contains(IRAccess.NATIVE)) {
                o.ln();
                if (m.type.kind != IRType.Kind.VOID)
                    o.w("return ");
                o.w("await env.callNative(sc,env,t,").w(methodName(m));
                i = 0;
                if (!m.access.contains(IRAccess.STATIC)) {
                    o.w(",p" + i);
                    i++;
                }
                for (final IRType ignored : m.argumentsTypes)
                    o.w(",p" + i++);
                o.w(");");
            } else
                try {
                    write(new SW(o, 2), m.instructions);
                } catch (final RuntimeException e) {
                    m.print();
                    throw e;
                }
            o.st(1).ln().w("},");
        }
        o.st(0).ln().w("}").ln();
    }

    public static void write(final W w, final ArrayList<Inst> instructions) {
        if (instructions.isEmpty())
            return;
        final ArrayList<String> scopeVars = new ArrayList<>();
        boolean hasJumps = false;
        for (final Inst inst : instructions)
            switch (inst.opcode) {
                case IF_EQ:
                case IF_NE:
                case IF_GT:
                case IF_GE:
                case IF_LT:
                case IF_LE:
                case IF_NULL:
                case IF_NONNULL:
                case GOTO:
                case CATCH:
                    hasJumps = true;
                    break;
                default:
                    String v = null;
                    if (inst.output instanceof IRRegister)
                        v = "r" + ((IRRegister) inst.output).index;
                    if (inst.output instanceof IRTmp)
                        v = "t" + ((IRTmp) inst.output).index;
                    if (v != null && !scopeVars.contains(v))
                        scopeVars.add(v);
                    break;
            }
        w.ln().w("let c,err");
        if (hasJumps)
            w.w(",cursor=0");
        for (final String var : scopeVars) {
            w.w(",");
            w.w(var);
        }
        w.w(";");
        if (hasJumps) {
            w.ln().w("while(true)");
            w.st(1).ln().w("switch(cursor){");
            if (!instructions.isEmpty() && instructions.get(0).opcode != Opcode.ANCHOR)
                w.st(2).ln().w("case 0:").st(3);
        }
        final ArrayList<IRExc> startTry = new ArrayList<>(), inTry = new ArrayList<>();
        final Runnable closeTry = () -> {
            if (inTry.isEmpty())
                return;
            final Iterator<IRExc> iter = inTry.iterator();
            w.st(3).ln().w("}catch(e){").st(4);
            boolean catchAll = false;
            while (iter.hasNext()) {
                final IRExc o = iter.next();
                iter.remove();
                startTry.add(o);
                if (catchAll)
                    continue;
                w.ln();
                if (o.cls != null)
                    w.w("if (e instanceof " + o.cls + "){");
                w.w("err=e;cursor=" + o.anchor.id + ";break;");
                if (o.cls != null)
                    w.w("}");
                else
                    catchAll = true;
            }
            if (!catchAll)
                w.ln().w("throw e;");
            w.st(3).ln().w("}");
        };
        for (final Inst inst : instructions) {
            if (inst.opcode != Opcode.TRY && inst.opcode != Opcode.CATCH && inst.opcode != Opcode.ANCHOR) {
                if (!startTry.isEmpty()) {
                    if (inTry.isEmpty()) {
                        w.ln().w("try{").st(4);
                        inTry.addAll(startTry);
                        startTry.clear();
                    }
                }
                w.ln();
            }
            switch (inst.opcode) {
                case ANCHOR:
                    closeTry.run();
                    if (hasJumps)
                        w.st(2).ln().w("case " + of(inst.params[0]) + ':').st(3);
                    break;
                case TRY:
                    startTry.add(0, (IRExc) inst.params[0]);
                    break;
                case CATCH:
                    if (startTry.remove((IRExc) inst.params[0]))
                        break;
                    closeTry.run();
                    if (startTry.remove((IRExc) inst.params[0]))
                        break;
                    throw new RuntimeException("Illegal state of try catch ...");
                case ADD:
                case SUBTRACT:
                case MULTIPLY:
                case DIVIDE:
                case REMAINDER:
                case SHIFT_LEFT:
                case SHIFT_RIGHT:
                case UNSIGNED_SHIFT_RIGHT:
                    if ((inst.opcode == Opcode.DIVIDE || inst.opcode == Opcode.REMAINDER) && (inst.params[2] == IRType.Kind.INT || inst.params[2] == IRType.Kind.LONG)) {
                        w.w("if(");
                        w.w(of(inst.params[1]));
                        w.w("===0");
                        if (inst.params[2] == IRType.Kind.LONG)
                            w.w("n");
                        w.w(")");
                        w.w("await divByZero(sc.class_loader,env,t);");
                        w.ln();
                    }
                    w.w(of(inst.output));
                    w.w("=");
                    switch ((IRType.Kind) inst.params[2]) {
                        case INT: w.w(inst.opcode == Opcode.MULTIPLY ? "Math.imul(" : "("); break;
                        case LONG: w.w(inst.opcode == Opcode.DIVIDE || inst.opcode == Opcode.REMAINDER ? "(" : inst.opcode == Opcode.UNSIGNED_SHIFT_RIGHT ? "BigInt.asIntN(64,BigInt.asUintN(64," : "BigInt.asIntN(64,"); break;
                        case FLOAT: w.w("Math.fround("); break;
                    }
                    w.w(of(inst.params[0]));
                    switch (inst.opcode) {
                        case ADD: w.w("+"); break;
                        case SUBTRACT: w.w("-"); break;
                        case MULTIPLY: w.w(inst.params[2] == IRType.Kind.INT ? "," : "*"); break;
                        case DIVIDE: w.w("/"); break;
                        case REMAINDER: w.w("%"); break;
                        case SHIFT_LEFT: w.w("<<"); if (inst.params[2] == IRType.Kind.LONG) w.w("BigInt("); break;
                        case SHIFT_RIGHT: w.w(">>"); if (inst.params[2] == IRType.Kind.LONG) w.w("BigInt("); break;
                        case UNSIGNED_SHIFT_RIGHT:
                            if (inst.params[2] == IRType.Kind.LONG)
                                w.w(")");
                            w.w(">>");
                            if (inst.params[2] == IRType.Kind.LONG)
                                w.w("(BigInt(");
                            else if (inst.params[2] == IRType.Kind.INT)
                                w.w(">");
                            break;
                    }
                    w.w(of(inst.params[1]));
                    switch ((IRType.Kind) inst.params[2]) {
                        case INT: w.w(inst.opcode == Opcode.MULTIPLY ? ")" : ") | 0"); break;
                        case LONG:
                            if (inst.opcode == Opcode.SHIFT_LEFT || inst.opcode == Opcode.SHIFT_RIGHT)
                                w.w("&0x3f)");
                            else if (inst.opcode == Opcode.UNSIGNED_SHIFT_RIGHT)
                                w.w(")&63n)");
                            w.w(")");
                            break;
                        case FLOAT: w.w(")"); break;
                    }
                    w.w(";");
                    break;

                case INT2LONG: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=BigInt(");
                    w.w(of(inst.params[0]));
                    w.w(");");
                    break;
                }

                case LONG2INT: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=Number(BigInt.asIntN(32,");
                    w.w(of(inst.params[0]));
                    w.w(")) | 0;");
                    break;
                }

                case STORE: {
                    w.w(of(inst.output));
                    w.w("=");
                    w.w(of(inst.params[0]));
                    w.w(";");
                    break;
                }
                case GET_STATIC: {
                    w.w(of(inst.output));
                    w.w("=");
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w("await env.getField(");
                    w.w("await env.getClass(sc.class_loader,\"");
                    w.w(ref.cls);
                    w.w("\"),\"");
                    w.w(ref.name);
                    w.w("\");");
                    break;
                }
                case PUT_STATIC: {
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w("env.setField(");
                    w.w("await env.getClass(sc.class_loader,\"");
                    w.w(ref.cls);
                    w.w("\"),\"");
                    w.w(ref.name);
                    w.w("\",");
                    w.w(of(inst.params[1]));
                    w.w(");");
                    break;
                }
                case GET_FIELD: {
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w(of(inst.output));
                    w.w("=await env.getField(");
                    w.w(of(inst.params[1]));
                    w.w(",\"");
                    w.w(ref.name);
                    w.w("\");");
                    break;
                }
                case PUT_FIELD: {
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w("await env.setField(");
                    w.w(of(inst.params[1]));
                    w.w(",\"");
                    w.w(ref.name);
                    w.w("\",");
                    w.w(of(inst.params[2]));
                    w.w(");");
                    break;
                }
                case ARRAY_LENGTH:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=await env.arrLen(");
                    w.w(of(inst.params[0]));
                    w.w(");");
                    break;
                case ARRAY_GET:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=await env.arrGet(");
                    w.w(of(inst.params[0])).w(",").w(of(inst.params[1]));
                    w.w(");");
                    break;
                case ARRAY_SET:
                    w.w("await env.arrSet(");
                    w.w(of(inst.params[0])).w(",").w(of(inst.params[1])).w(",").w(of(inst.params[2]));
                    w.w(");");
                    break;
                case NEW_ARRAY:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=await env.newArr(");
                    w.w(of(inst.params[1]));
                    w.w(");");
                    break;
                case GOTO:
                    w.w("cursor=" + ((IRAnchor) inst.params[0]).id + ';');
                    w.w("break;");
                    break;
                case IF_NULL: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("===null){");
                    w.w("cursor=" + ((IRAnchor) inst.params[1]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_NONNULL: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("!==null){");
                    w.w("cursor=" + ((IRAnchor) inst.params[1]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }

                case IF_EQ:
                case IF_NE:
                case IF_GT:
                case IF_LT:
                case IF_GE:
                case IF_LE:
                {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    switch (inst.opcode) {
                        case IF_EQ: w.w("==="); break;
                        case IF_NE: w.w("!=="); break;
                        case IF_GE: w.w(">="); break;
                        case IF_LE: w.w("<="); break;
                        case IF_GT: w.w(">"); break;
                        case IF_LT: w.w("<"); break;
                    }
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + ((IRAnchor) inst.params[2]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case COMPARE: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=");
                    w.w(of(inst.params[0]) + ">" + of(inst.params[1]) + "?1:");
                    w.w(of(inst.params[0]) + "<" + of(inst.params[1]) + "?-1:0;");
                    break;
                }
                case ALLOCATE:
                    if (inst.output != null) {
                        w.w(of(inst.output));
                        w.w("=");
                    }
                    w.w("await env.alloc(await env.getClass(sc.class_loader,");
                    w.w(of(inst.params[0]));
                    w.w("));");
                    break;
                case INVOKE_STATIC:
                case INVOKE_SPECIAL:
                case INVOKE_VIRTUAL:
                case INVOKE_INTERFACE: {
                    final Iterator<Object> params = new ArrIterator<>(inst.params);
                    final IRMethodRef ref = (IRMethodRef) params.next();
                    if (inst.opcode == Opcode.INVOKE_VIRTUAL || inst.opcode == Opcode.INVOKE_INTERFACE)
                        w.w("c=env.virtualMethod(t0.cls," + methodName(ref) + ");").ln();
                    else
                        w.w("c=await env.getClass(sc.class_loader,\"" + ref.cls + "\");").ln();
                    if (inst.output != null) {
                        w.w(of(inst.output));
                        w.w("=");
                    }
                    w.w("await c[");
                    w.w(methodName(ref));
                    w.w("](c,env,t");
                    if (inst.opcode != Opcode.INVOKE_STATIC)
                        w.w("," + of(params.next()));
                    while (params.hasNext()) {
                        w.w(",");
                        w.w(of(params.next()));
                    }
                    w.w(");");
                    break;
                }
                case RETURN: {
                    w.w("return");
                    final Iterator<Object> iter = new ArrIterator<>(inst.params);
                    if (iter.hasNext()) {
                        w.w(" ");
                        w.w(of(iter.next()));
                        if (iter.hasNext())
                            throw new RuntimeException("There's more than 1 return parameters");
                    }
                    w.w(";");
                    break;
                }
                case THROW:
                    w.w("throw ");
                    w.w(of(inst.params[0]));
                    w.w(";");
                    break;
                case MONITOR_ENTER:
                    w.w("await env.monitorEnter(t,");
                    w.w(of(inst.params[0]));
                    w.w(");");
                    break;
                case MONITOR_EXIT:
                    w.w("await env.monitorExit(t,");
                    w.w(of(inst.params[0]));
                    w.w(");");
                    break;
                case CHECK_CAST: break;
                case INSTANCEOF:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=instanceOf(").w(of(inst.params[0])).w(",").w(of(inst.params[1])).w(");");
                    break;
                default:
                    throw new RuntimeException("Unknown opcode " + inst.opcode);
            }
        }
        closeTry.run();
        if (hasJumps) {
            w.st(2).ln().w("default:throw new SyntaxError();");
            w.st(1).ln().w("}");
        }
    }

    public static String of(final Object arg) {
        if (arg == Const.THIS)
            return "this";
        if (arg == Const.EXCEPTION)
            return "err";
        if (arg instanceof IRRegister)
            return "r" + ((IRRegister) arg).index;
        if (arg instanceof IRTmp)
            return "t" + ((IRTmp) arg).index;
        if (arg instanceof IRParameter)
            return "p" + ((IRParameter) arg).index;
        if (arg instanceof IRByte)
            return Byte.toString(((IRByte) arg).n);
        if (arg instanceof IRInt)
            return Integer.toString(((IRInt) arg).n);
        if (arg instanceof IRLong)
            return ((IRLong) arg).n + "n";
        if (arg instanceof IRFloat)
            return Float.toString(((IRFloat) arg).n);
        if (arg instanceof IRDouble)
            return Double.toString(((IRDouble) arg).n);
        if (arg instanceof String)
            return '"' + ((String) arg).replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"")
                    .replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r") + '"';
        return String.valueOf(arg);
    }
}