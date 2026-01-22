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
        public final PrintStream ps;

        public MW(final PrintStream ps) { this.ps = ps; }

        @Override
        public W w(final String str) {
            ps.print(str);
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
            case VOID:
                b.append("void");
                break;
            case BOOLEAN:
                b.append("boolean");
                break;
            case INT:
                b.append("int");
                break;
            case LONG:
                b.append("long");
                break;
            case LITERAL:
                b.append(type.cls);
                break;
            default:
                throw new RuntimeException("Unknown kind: " + type.kind);
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
        o.ln().w("cls:\"" + cls.name + "\",");
        o.ln().w("super_cls:\"" + cls.superName + "\",");
        o.ln().w("declared_fields:{").st(2);
        for (final IRField f : cls.fields) {
            o.ln().w("\"_" + f.name + "\":\"").w(encType(f.type)).w("\",");
        }
        o.st(1).ln().w("},");
        for (final IRMethod m : cls.methods) {
            o.ln().w(methodName(m) + ":async function(sc,env,t");
            int i = 0;
            if (!m.access.contains(IRAccess.STATIC)) {
                o.w(",p" + i);
                i++;
            }
            for (final IRType ignored : m.argumentsTypes)
                o.w(",p" + i++);
            o.w("){").st(2);
            if (m.access.contains(IRAccess.NATIVE))
                o.ln().w("// native");
            else
                write(new SW(o, 2), m.instructions);
            o.st(1).ln().w("},");
        }
        o.ln().w("class_loader:null");
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
                case IF_GE:
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
        for (final String var : scopeVars) {
            w.w(",");
            w.w(var);
        }
        w.w(";");
        if (hasJumps) {
            w.ln().w("let cursor=0;");
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
                    w.w(of(inst.output));
                    w.w("=");
                    w.w(of(inst.params[0]));
                    w.w("+");
                    w.w(of(inst.params[1]));
                    w.w(";");
                    break;
                case SUBTRACT:
                    w.w(of(inst.output));
                    w.w("=");
                    w.w(of(inst.params[0]));
                    w.w("-");
                    w.w(of(inst.params[1]));
                    w.w(";");
                    break;

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
                    w.w("env.getField(");
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
                    w.w(of(inst.output));
                    w.w("=");
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w(of(inst.params[1]));
                    w.w(".");
                    w.w(ref.name);
                    w.w(";");
                    break;
                }
                case PUT_FIELD: {
                    final IRFieldRef ref = (IRFieldRef) inst.params[0];
                    w.w(of(inst.params[1]));
                    w.w(".");
                    w.w(ref.name);
                    w.w("=");
                    w.w(of(inst.params[2]));
                    w.w(";");
                    break;
                }
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
                case IF_EQ: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("===");
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + ((IRAnchor) inst.params[2]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_NE: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("!==");
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + ((IRAnchor) inst.params[2]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_GE: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w(">==");
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + ((IRAnchor) inst.params[2]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_LE: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("<==");
                    w.w(of(inst.params[1]));
                    w.w("){");
                    w.w("cursor=" + ((IRAnchor) inst.params[2]).id + ';');
                    w.w("break;");
                    w.w("}");
                    break;
                }
                case IF_LT: {
                    w.w("if(");
                    w.w(of(inst.params[0]));
                    w.w("<");
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
                    w.w("new ");
                    w.w(of(inst.params[0]));
                    w.w("();");
                    break;
                case INVOKE_STATIC:
                case INVOKE_SPECIAL:
                case INVOKE_VIRTUAL: {
                    final Iterator<Object> params = new ArrIterator<>(inst.params);
                    final IRMethodRef ref = (IRMethodRef) params.next();
                    if (inst.opcode == Opcode.INVOKE_VIRTUAL)
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
                case MONITOR_ENTER: break;
                case MONITOR_EXIT: break;
                case CHECK_CAST: break;
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
        if (arg instanceof IRInt)
            return Integer.toString(((IRInt) arg).n);
        if (arg instanceof String)
            return '"' + (String) arg + '"';
        return String.valueOf(arg);
    }
}