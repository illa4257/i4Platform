package i4l;

import i4l.common.*;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class I4LJavaExport {
    public static void export(final List<Object> code, final PrintStream s) throws Exception {
        new I4LJavaExport(code, null, s);
    }

    public static void export(final List<Object> code, final File dir) throws Exception {
        new I4LJavaExport(code, dir, null);
    }

    public String pkg = "";
    public ArrayList<String> imports = new ArrayList<>();
    public final File dir;

    public PrintStream s;

    public I4LJavaExport(final List<Object> code, final File dir, final PrintStream stream) throws Exception {
        this.dir = dir;
        this.s = stream;

        f("Global");
        for (final Object o : code) {
            if (o instanceof I4LPkg) {
                pkg = ((I4LPkg) o).path;

                //s = f(((I4LPkg) o).path);
                //s.println("package " + ((I4LPkg) o).path + ";");
                continue;
            }
            if (o instanceof I4LImport) {
                imports.add(((I4LImport) o).path);
                //s.println("import " + ((I4LImport) o).path + ";");
                continue;
            }
            if (o instanceof I4LClass) {
                exportClass((I4LClass) o, 0, true);
                continue;
            }

            throw new Exception("Unknown class " + o.getClass().getName());
        }
    }

    public void exportClass(final I4LClass cls, int t, final boolean file) throws Exception {
        if (file) {
            String cn = null;
            final int max = cls.tags.size();
            for (int i = 0; i < max; i++)
                if (cls.tags.get(i).equals("class")) {
                    i++;
                    if (i < max)
                        cn = cls.tags.get(i);
                    break;
                }
            if (cn == null)
                throw new Exception("Unnamed class");
            f(cn);

            if (!pkg.isEmpty())
                s.println("package " + pkg + ';');

            for (final String p : imports)
                s.println("import " + p + ';');

            imports.clear();
        }

        s.println(repeat("\t", t) + join(cls.tags, " ") + " {");
        t++;
        for (final Object o : cls.code) {
            if (o instanceof I4LNewVar) {
                final I4LNewVar nv = (I4LNewVar) o;
                s.println(repeat("\t", t) + join(nv.params, " "));
                t++;
                final int m = nv.vars.size();
                for (int i = 0; i < m; i++) {
                    if (i > 0)
                        s.println(',');
                    final I4LNewVar.Var v = nv.vars.get(i);
                    s.print(repeat("\t", t) + v.name);
                    if (v.value != null) {
                        s.print(" = ");
                        exportData(v.value, t + 1);
                    }
                }
                s.println(';');
                t--;
                continue;
            }

            if (o instanceof I4LMethod) {
                final I4LMethod m = (I4LMethod) o;
                s.print(repeat("\t", t) + join(m.tags, " ") + " " + m.name + '(');
                final int l = m.args.size();
                for (int i = 0; i < l; i++) {
                    if (i > 0)
                        s.print(", ");
                    s.print(join(m.args.get(i).params, " "));
                }
                s.print(") ");
                if (!m.exceptions.isEmpty())
                    s.print("throws " + join(m.exceptions, ", ") + ' ');
                s.println("{");
                t++;
                exportCode(m.code, t);
                t--;
                s.println(repeat("\t", t) + '}');
                continue;
            }

            if (o instanceof I4LClass) {
                exportClass((I4LClass) o, t, false);
                s.println();
                continue;
            }

            throw new Exception("Unknown class " + o.getClass().getName());
        }
        t--;
        s.print(repeat("\t", t) + "}");
    }

    public boolean kwc(final I4LKeyword kw) {
        if (kw.operation instanceof I4LKeyword)
            return kwc((I4LKeyword) kw.operation);
        return kw.operation instanceof I4LStatement;
    }

    public void exportCode(final List<Object> code, int t) throws Exception {
        for (final Object o : code) {
            s.print(repeat("\t", t));
            exportData(o, t);
            if (
                    !(o instanceof I4LStatement) &&
                    !(o instanceof I4LIfElse) &&
                            (!(o instanceof I4LKeyword) || !kwc((I4LKeyword) o)) &&
                            !(o instanceof I4LCode) &&
                            !(o instanceof I4LMark) &&
                            !(o instanceof I4LCase)
            )
                s.println(";");
            else
                s.println();
        }
    }

    public void exportData(final Object o, int t) throws Exception {
        if (o instanceof Boolean || o instanceof Integer || o instanceof String) {
            s.print(o);
            return;
        }

        if (o instanceof Character) {
            final char ch = (char) o;
            switch (ch) {
                case '\\':
                    s.print("'\\\\'");
                    break;
                case '\'':
                    s.print("'\\''");
                    break;
                case '\r':
                    s.print("'\\r'");
                    break;
                case '\n':
                    s.print("'\\n'");
                    break;
                case '\b':
                    s.print("'\\b'");
                    break;
                case '\t':
                    s.print("'\\t'");
                    break;
                default:
                    s.print("'" + ch + "'");
                    break;
            }
            return;
        }

        if (o instanceof I4LString) {
            s.print('"');
            for (final char ch : ((I4LString) o).string.toCharArray())
                switch (ch) {
                    case '"':
                        s.print("\\\"");
                        break;
                    case '\\':
                        s.print("\\\\");
                        break;
                    default:
                        s.print(ch);
                        break;
                }
            s.print('"');
            return;
        }

        if (o instanceof I4LEmpty)
            return;

        if (o instanceof I4LNegative) {
            s.print('-');
            exportData(((I4LNegative) o).value, t);
            return;
        }

        if (o instanceof I4LSubArg) {
            s.print('(');
            exportData(((I4LSubArg) o).value, t + 1);
            s.print(')');
            return;
        }

        if (o instanceof I4LCase) {
            s.print("case ");
            exportData(((I4LCase) o).value, t + 1);
            s.print(':');
            return;
        }

        if (o instanceof I4LCode) {
            final ArrayList<Object> c = ((I4LCode) o).code;
            s.print('{');
            if (!c.isEmpty()) {
                s.println();
                exportCode(c, t + 1);
                s.println();
                s.println(repeat("\t", t));
            }
            s.print('}');
            return;
        }

        if (o instanceof I4LMark) {
            s.print(((I4LMark) o).name + ':');
            return;
        }

        if (o instanceof I4LInstanceOf) {
            exportData(((I4LInstanceOf) o).object, t + 1);
            s.print(" instanceof ");
            s.print(((I4LInstanceOf) o).cn);
            return;
        }

        if (o instanceof I4LComment) {
            final String c = ((I4LComment) o).comment;
            if (((I4LComment) o).single) {
                s.print("//" + c);
            } else {
                s.print("/*");
                s.print(c);
                s.print("*/");
            }
            return;
        }

        if (o instanceof I4LCombine) {
            final I4LCombine c = (I4LCombine) o;
            exportData(c.v1, t);
            s.println(" " + c.operation);
            s.print(repeat("\t", t + 1));
            exportData(c.v2, t + 2);
            return;
        }

        if (o instanceof I4LConvert) {
            s.print("(" + ((I4LConvert) o).to + ") ");
            exportData(((I4LConvert) o).value, t);
            return;
        }

        if (o instanceof I4LGet) {
            s.print(((I4LGet) o).path);
            return;
        }

        if (o instanceof I4LKeyword) {
            if (((I4LKeyword) o).operation == null) {
                s.print(((I4LKeyword) o).name);
                return;
            }
            s.print(((I4LKeyword) o).name + " ");
            exportData(((I4LKeyword) o).operation, t);
            return;
        }

        if (o instanceof I4LCall) {
            final I4LCall c = (I4LCall) o;
            exportData(c.target, t);
            s.print('(');
            t++;
            final int m = c.args.size();
            for (int i = 0; i < m; i++) {
                if (i > 0)
                    s.println(",");
                else
                    s.println();
                s.print(repeat("\t", t));
                exportData(c.args.get(i), t);
            }
            t--;
            if (m > 0) {
                s.println();
                s.print(repeat("\t", t));
            }
            s.print(")");
            return;
        }

        if (o instanceof I4LCompare) {
            exportData(((I4LCompare) o).v1, t);
            s.println(' ' + ((I4LCompare) o).check + ' ');
            s.print(repeat("\t", t + 1));
            exportData(((I4LCompare) o).v2, t);
            return;
        }

        if (o instanceof I4LSet) {
            if (((I4LSet) o).value.equals(1) && ((I4LSet) o).operation != '=') {
                s.print("" + ((I4LSet) o).operation + ((I4LSet) o).operation);
                exportData(((I4LSet) o).path, t);
                return;
            }
            exportData(((I4LSet) o).path, t);
            if (((I4LSet) o).operation == '=')
                s.println(" = ");
            else
                s.println(" " + ((I4LSet) o).operation + "= ");
            s.print(repeat("\t", t + 1));
            exportData(((I4LSet) o).value, t + 2);
            return;
        }

        if (o instanceof I4LSetAfter) {
            exportData(((I4LSetAfter) o).path, t);
            s.print("" + ((I4LSetAfter) o).operation + ((I4LSetAfter) o).operation);
            return;
        }

        if (o instanceof I4LNewVar) {
            final I4LNewVar nv = (I4LNewVar) o;
            s.println(join(nv.params, " "));
            t++;
            int i = 0;
            for (final I4LNewVar.Var v : nv.vars) {
                if (i > 0) {
                    s.println(",");
                }
                s.print(repeat("\t", t) + v.name);
                if (v.value != null) {
                    s.print(" = ");
                    exportData(v.value, t + 1);
                }
                i++;
            }
            return;
        }

        if (o instanceof I4LStatement) {
            final I4LStatement a = (I4LStatement) o;
            s.print(a.name + ' ');
            if (a.begin != null && !a.begin.isEmpty()) {
                s.print('(');
                t++;
                final int l = a.begin.size();
                for (int i = 0; i < l; i++) {
                    if (i > 0)
                        s.print(';');
                    s.println();
                    s.print(repeat("\t", t));
                    exportData(a.begin.get(i), t);
                }
                t--;
                s.println();
                s.print(repeat("\t", t) + ") ");
            }
            if (a.code.isEmpty()) {
                s.print("{}");
                return;
            }
            s.println("{");
            t++;
            exportCode(a.code, t);
            t--;
            s.print(repeat("\t", t) + "}");
            return;
        }

        if (o instanceof I4LIfElse) {
            final I4LIfElse a = (I4LIfElse) o;
            s.print("if ");
            s.print('(');
            t++;
            if (a.begin.size() != 1)
                throw new Exception("Begin size is not equals 1");
            s.println();
            s.print(repeat("\t", t));
            exportData(a.begin.get(0), t);
            t--;
            s.println();
            s.print(repeat("\t", t) + ") ");
            if (a.code.isEmpty()) {
                s.print("{}");
                return;
            }
            s.println("{");
            t++;
            exportCode(a.code, t);
            t--;
            s.println(repeat("\t", t) + "} else {");
            t++;
            exportCode(a.elseCode, t);
            t--;
            s.print(repeat("\t", t) + '}');
            return;
        }

        if (o instanceof I4LCodeIf) {
            final I4LCodeIf ci = (I4LCodeIf) o;
            exportData(ci.check, t);
            s.println(" ?");
            t++;
            s.print(repeat("\t", t));
            exportData(ci.then, t);
            s.println(" :");
            exportData(ci.el, t);
            return;
        }

        if (o instanceof I4LForEach) {
            final I4LForEach f = (I4LForEach) o;
            s.print(join(f.params, " ") + " : ");
            exportData(f.of, t + 1);
            return;
        }

        if (o instanceof I4LSub) {
            exportData(((I4LSub) o).object, t + 1);
            s.print('.');
            exportData(((I4LSub) o).path, t + 1);
            return;
        }

        throw new Exception("Unknown class " + o.getClass().getName() + " : " + o);
    }

    public String join(final List<?> l, final String d) {
        final int m = l.size();
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < m; i++) {
            if (i > 0)
                b.append(d);
            b.append(l.get(i));
        }
        return b.toString();
    }

    public String repeat(final String str, int number) {
        final StringBuilder b = new StringBuilder(str.length() * number);
        for (; number > 0; number--)
            b.append(str);
        return b.toString();
    }

    public void f(final String cls) throws Exception {
        if (dir != null) {
            if (s != null)
                s.close();
            final File d = new File(dir, pkg.replaceAll("\\.", "/"));
            if (!d.exists())
                d.mkdirs();
            final File f = new File(d, cls + ".java");
            s = new PrintStream(f);
        }
    }
}