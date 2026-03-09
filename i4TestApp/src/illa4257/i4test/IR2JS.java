package illa4257.i4test;

import illa4257.i4Utils.ir.*;
import illa4257.i4Utils.lists.ArrIterator;
import illa4257.i4Utils.ir.IRExc;
import illa4257.i4Utils.str.Str;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IR2JS {
    public interface W {
        W w(final String str);
        default W t(final int n) { return this; }
        int st();
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

        @Override public int st() { return 0; }
    }

    public static class PSW implements W {
        public final PrintStream ps;
        public int nt = 0;

        public PSW(final PrintStream ps) { this.ps = ps; }

        @Override
        public int st() { return nt; }

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
        public int st() {
            return w.st() - o;
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

    public static String encArg(final IRType type) {
        return type.toJava()
                .replaceAll("_", "_1")
                .replaceAll(";", "_2")
                .replaceAll("\\[", "_3");
    }

    public static StringBuilder encArgs(final StringBuilder b, final List<IRType> args, final IRType type) {
        Str.join(b.append("__"), "", args, (t, sb) -> sb.append(encArg(t)))
                .append(encArg(type));
        return b;
    }

    public static String encArgs(final List<IRType> args, final IRType type) {
        final StringBuilder b = Str.builder();
        try {
            return encArgs(b, args, type).toString();
        } finally {
            Str.recycle(b);
        }
    }

    public static String methodName(final String methodName, final List<IRType> args, final IRType type) {
        final StringBuilder b = Str.builder();
        try {
            return encArgs(b.append('"').append(methodName), args, type).append('"').toString();
        } finally {
            Str.recycle(b);
        }
    }

    public static String methodName(final IRMethod method) {
        return methodName(method.name, method.argumentsTypes, method.type);
    }

    public static String methodName(final IRMethodRef method) {
        return methodName(method.name, method.params, method.type);
    }

    public static W write(final W o, final IRField f) {
        o.w(escapeStr(f.name)).w(":{type:").w(escapeStr(encType(f.type)))
                .w(",flags:").w(Integer.toString(IRAccess.toJava(f.access)));
        if (f.value != null)
            o.w(",value:").w(f.value instanceof String ? escapeStr((String) f.value) : of(f.value));
        return o.w("}");
    }

    public static void write(final W o, final IRClass cls) {
        o.w("{").st(1);
        o.ln().w("name:\"" + cls.name + "\",");
        if (cls.superName != null)
            o.ln().w("super_cls:\"" + cls.superName + "\",");
        o.ln().w("fields:{").st(2);
        for (final IRField f : cls.fields)
            write(o.ln(), f).w(",");
        o.st(1).ln().w("},");
        for (final IRMethod m : cls.methods) {
            o.ln().w(methodName(m) + ":async function(");
            int i = 0;
            o.w("sc,env,t");
            if (!m.access.contains(IRAccess.STATIC)) {
                o.w(",a" + i);
                i++;
            }
            for (final IRType ignored : m.argumentsTypes)
                o.w(",a" + i++);
            o.w("){try{await env.traceEnter(sc,t,").w(escapeStr(m.name)).w(");").st(2);
            if (m.access.contains(IRAccess.NATIVE)) {
                o.ln();
                if (m.type.kind != IRType.Kind.VOID)
                    o.w("return ");
                o.w("await env.callNative(sc,env,t,\"").w(m.name).w("\",\"")
                        .w(encArgs(m.argumentsTypes, m.type)
                                .replaceAll("[/,()]", "_"))
                        .w("\"");
                i = 0;
                if (!m.access.contains(IRAccess.STATIC)) {
                    o.w(",a" + i);
                    i++;
                }
                for (final IRType ignored : m.argumentsTypes)
                    o.w(",a" + i++);
                o.w(");");
            } else
                try {
                    write(new SW(o, 2), m.instructions);
                } catch (final RuntimeException e) {
                    System.err.println("#" + m.name);
                    m.print();
                    throw e;
                }
            o.st(1).ln().w("}finally{await env.traceExit(t);}},");
        }
        o.st(0).ln().w("}");
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
                case LOOKUPSWITCH:
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
        w.ln().w("let c,err,h0");
        String cursorValue = "0";
        if (hasJumps) {
            if (!instructions.isEmpty() && instructions.get(0).opcode == Opcode.ANCHOR)
                cursorValue = of(instructions.get(0).params[0]);
            w.w(",cursor=").w(cursorValue);
        }
        for (final String var : scopeVars) {
            w.w(",");
            w.w(var);
        }
        w.w(";");
        if (hasJumps) {
            w.ln().w("while(true)");
            w.st(1).ln().w("switch(cursor){");
            if (!instructions.isEmpty() && instructions.get(0).opcode != Opcode.ANCHOR)
                w.st(2).ln().w("case ").w(cursorValue).w(":").st(3);
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
                    w.w("if(instanceOf(e,").w(escapeStr(o.cls)).w(")){");
                else
                    w.w("if(e instanceof Error)throw e;").ln();
                w.w("err=e;cursor=" + of(o.anchor.id) + ";break;");
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
                        case INT: w.w(inst.opcode == Opcode.MULTIPLY ? ")" : ")|0"); break;
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

                case INT2BYTE: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=(").w(of(inst.params[0])).w("<<24)>>24;");
                    break;
                }

                case INT2LONG:
                case INT2FLOAT:
                case INT2CHAR:
                {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    switch (inst.opcode) {
                        case INT2LONG: w.w("=BigInt("); break;
                        case INT2FLOAT: w.w("=Math.fround("); break;
                        case INT2CHAR: w.w("=String.fromCharCode("); break;
                    }
                    w.w(of(inst.params[0]));
                    if (inst.opcode == Opcode.INT2CHAR)
                        w.w("&0xFFFF");
                    w.w(");");
                    break;
                }

                case INT2SHORT:
                {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=(").w(of(inst.params[0])).w("<<16)>>16;");
                    break;
                }

                case INT2DOUBLE:
                case FLOAT2DOUBLE:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=").w(of(inst.params[0])).w(";");
                    break;

                case LONG2INT: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=Number(BigInt.asIntN(32,").w(of(inst.params[0])).w(")) | 0;");
                    break;
                }

                case LONG2FLOAT: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=Math.fround(Number(").w(of(inst.params[0])).w("));");
                    break;
                }

                case LONG2DOUBLE:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=Number(").w(of(inst.params[0])).w(");");
                    break;

                case FLOAT2INT: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=isNaN(").w(of(inst.params[0])).w(")?NaN:~~").w(of(inst.params[0])).w(";");
                    break;
                }

                case DOUBLE2FLOAT:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=Math.fround(").w(of((inst.params[0]))).w(");");
                    break;

                case DOUBLE2INT:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=d2i(").w(of(inst.params[0])).w(");");
                    break;

                case FLOAT2LONG:
                case DOUBLE2LONG:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=d2l(").w(of(inst.params[0])).w(");");
                    break;

                case NEGATIVE:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output)).w("=-").w(of(inst.params[0])).w(";");
                    break;

                case STORE: {
                    w.w(of(inst.output)).w("=").w(of(inst.params[0])).w(";");
                    break;
                }
                case GET_STATIC: {
                    w.w(of(inst.output));
                    w.w("=");
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w("await env.getField(");
                    w.w("await env.getClass(sc.class_loader,t,\"");
                    w.w(ref.cls);
                    w.w("\"),\"");
                    w.w(ref.name);
                    w.w("\");");
                    break;
                }
                case PUT_STATIC: {
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w("env.setField(");
                    w.w("await env.getClass(sc.class_loader,t,\"");
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
                    w.w("cursor=" + of(((IRAnchor) inst.params[0]).id) + ';');
                    w.w("break;");
                    break;
                case IF_NULL: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("==null){");
                    w.w("cursor=" + of(((IRAnchor) inst.params[1]).id) + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_NONNULL: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("!=null){");
                    w.w("cursor=" + of(((IRAnchor) inst.params[1]).id) + ';');
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
                        case IF_EQ: w.w("=="); break;
                        case IF_NE: w.w("!="); break;
                        case IF_GE: w.w(">="); break;
                        case IF_LE: w.w("<="); break;
                        case IF_GT: w.w(">"); break;
                        case IF_LT: w.w("<"); break;
                    }
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + of(((IRAnchor) inst.params[2]).id) + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }

                case OR:
                case XOR:
                case AND:
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=");
                    switch ((IRType.Kind) inst.params[2]) {
                        case INT: w.w("("); break;
                        case LONG: w.w("BigInt.asIntN(64,"); break;
                    }
                    w.w(of(inst.params[0]));
                    w.w(inst.opcode == Opcode.OR ? "|" : inst.opcode == Opcode.XOR ? "^" : "&");
                    w.w(of(inst.params[1]));
                    switch ((IRType.Kind) inst.params[2]) {
                        case INT: w.w(")|0"); break;
                        case LONG: w.w(")"); break;
                    }
                    w.w(";");
                    break;

                case COMPARE: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=");
                    w.w(of(inst.params[0]) + ">" + of(inst.params[1]) + "?1:");
                    w.w(of(inst.params[0]) + "<" + of(inst.params[1]) + "?-1:0;");
                    break;
                }

                case COMPARE_NAN: {
                    if (inst.output == null)
                        break;
                    w.w(of(inst.output));
                    w.w("=isNaN(").w(of(inst.params[0]) + ")||isNaN(" + of(inst.params[1]) + ")?" + of(inst.params[2]) + ":");
                    w.w(of(inst.params[0]) + ">" + of(inst.params[1]) + "?1:");
                    w.w(of(inst.params[0]) + "<" + of(inst.params[1]) + "?-1:0;");
                    break;
                }

                case ALLOCATE:
                    if (inst.output != null) {
                        w.w(of(inst.output));
                        w.w("=");
                    }
                    w.w("await env.alloc(await env.getClass(sc.class_loader,t,");
                    w.w(escapeStr((String) inst.params[0]));
                    w.w("),t);");
                    break;
                case INVOKE_STATIC:
                case INVOKE_SPECIAL:
                case INVOKE_VIRTUAL:
                case INVOKE_INTERFACE: {
                    final Iterator<Object> params = new ArrIterator<>(inst.params);
                    final IRMethodRef ref = (IRMethodRef) params.next();
                    if (inst.opcode == Opcode.INVOKE_VIRTUAL || inst.opcode == Opcode.INVOKE_INTERFACE)
                        w.w("c=virtualMethod(" + of(inst.params[1]) + ".cls," + methodName(ref) + ");").ln();
                    else
                        w.w("c=await env.getClass(sc.class_loader,t,\"" + ref.cls + "\");").ln();
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

                case INVOKE_DYNAMIC:
                    switch ((Opcode) inst.params[0]) {
                        case INVOKE_STATIC: {
                            w.w("throw new Error('INVOKE_DYNAMIC');");
                            break;
                        }
                        default:
                            throw new RuntimeException("Unknown opcode: " + inst.params[0]);
                    }


                    /*switch ((Opcode) inst.params[0]) {
                        case INVOKE_STATIC: {
                            w.w("c=await env.getClass(sc.class_loader,t,\"" + ((IRMethodRef) inst.params[1]).cls + "\");").ln();
                            break;
                        }
                        default:
                            throw new RuntimeException("Unknown opcode: " + inst.params[0]);
                    }
                    switch ((Opcode) inst.params[0]) {
                        case INVOKE_STATIC: {
                            final IRMethodRef ref = (IRMethodRef) inst.params[1];
                            w.w("h0=env.getField(await c[").w(methodName(ref)).w("](c,env,t");
                            w.w("),'target');").ln();
                            if (inst.output != null) {
                                w.w(of(inst.output));
                                w.w("=");
                            }
                            final IRMethodRef invoke = new IRMethodRef();
                            invoke.name = "invokeExact";
                            invoke.params.add(new IRType("java/lang/Object", 1));
                            invoke.type = new IRType("java/lang/Object");
                            w.w("h0.cls[").w(methodName(invoke)).w("](c,env,t,h0");

                            w.w(");");
                            break;
                        }
                        default:
                            throw new RuntimeException("Unknown opcode: " + inst.params[0]);
                    }*/
                    break;

                case RETURN: {
                    w.w("return");
                    final Iterator<Object> iter = new ArrIterator<>(inst.params);
                    if (iter.hasNext()) {
                        w.w(" ").w(of(iter.next()));
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
                    w.w("=instanceOf(").w(of(inst.params[0])).w(",").w(escapeStr((String) inst.params[1])).w(");");
                    break;

                case LOOKUPSWITCH: {
                    w.w("switch(").w(of(inst.params[0])).w("){").st(w.st() + 1);
                    final int[] keys = (int[]) inst.params[2];
                    final IRAnchor[] jumps = (IRAnchor[]) inst.params[3];
                    for (int i = 0; i < keys.length; i++)
                        w.ln().w("case ").w(Integer.toString(keys[i]))
                                .w(":cursor=").w(of(jumps[i].id)).w(";break;");
                    w.ln().w("default:cursor=").w(of(((IRAnchor) inst.params[1]).id)).w(";break;");
                    w.st(w.st() - 1).ln().w("}").ln().w("break;");
                    break;
                }
                case TABLESWITCH: {
                    w.w("switch(").w(of(inst.params[0])).w("){").st(w.st() + 1);
                    final IRAnchor[] jumps = (IRAnchor[]) inst.params[4];

                    for (int i = 0, low = (int) inst.params[2], high = (int) inst.params[3]; low <= high; low++, i++)
                        w.ln().w("case ").w(Integer.toString(low))
                                .w(":cursor=").w(of(jumps[i].id)).w(";break;");
                    w.ln().w("default:cursor=").w(of(((IRAnchor) inst.params[1]).id)).w(";break;");
                    w.st(w.st() - 1).ln().w("}").ln().w("break;");
                    break;
                }

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
        if (arg instanceof IRArg)
            return "a" + ((IRArg) arg).index;
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
        if (arg instanceof IRClassRef)
            return "await getClass(await env.getClass(sc.class_loader,t," + escapeStr(((IRClassRef) arg).cls) + "),env,t)";
        if (arg instanceof String)
            return "await Java_str(sc.class_loader,env,t," + escapeStr((String) arg) + ')';
        //return String.valueOf(arg);
        if (arg instanceof Byte || arg instanceof Short || arg instanceof Integer || arg instanceof Float || arg instanceof Double)
            return arg.toString();
        if (arg instanceof Long)
            return arg + "n";
        if (arg == null)
            return "null";
        throw new RuntimeException("Can't convert " + arg.getClass() + " > " + arg);
    }

    public static String escapeStr(final String str) {
        return '"' + str
                .replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\"", "\\\\\"")
                .replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r") + '"';
    }

    public static String encType(final IRType type) {
        final StringBuilder b = new StringBuilder();
        Str.repeat(b, "[", type.array);
        switch (type.kind) {
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
        return Str.getAndRecycle(b);
    }
}