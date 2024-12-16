package i4l;

import i4l.common.*;
import i4l.common.preprocessors.*;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class I4LParser {
    private final Reader r;

    public final List<Object> code;
    public final List<String> keywords;
    public Exception exception = null;
    public int line = 1, off = 0, i = 0;
    public final ArrayList<Character> buff = new ArrayList<>();

    public void clear() {
        for (; i > 0; i--)
            buff.remove(0);
    }

    public void back(int steps) throws Exception {
        if (steps > i)
            throw new Exception("It's can't be negative steps!");
        for (; steps > 0; steps--)
            if (buff.get(--i) == '\n')
                line--;
        off = 0;
        for (int i = this.i; i >= 0; i--)
            if (buff.get(i) == '\n')
                break;
            else
                off++;
    }

    public int read() throws Exception {
        if (i >= buff.size()) {
            final int ic = r.read();
            if (ic == -1)
                return -1;
            buff.add((char) ic);
            i++;
            if (ic == '\n') {
                line++;
                off = 0;
            } else
                off++;
            return ic;
        }
        final char r = buff.get(i++);
        if (r == '\n') {
            line++;
            off = 0;
        } else
            off++;
        return r;
    }

    public char r() throws Exception {
        final int i = read();
        if (i == -1)
            throw new Exception("END");
        return (char) i;
    }

    public int skip(final List<Character> chars) throws Exception {
        int n = 0;
        while (true) {
            final int i = read();
            if (i == -1)
                return n;
            if (chars.contains((char) i)) {
                n++;
                continue;
            }
            back(1);
            break;
        }
        return n;
    }

    public String readTo(final List<Character> chars) throws Exception {
        final StringBuilder b = new StringBuilder();

        while (true) {
            final int r = read();
            if (r == -1)
                break;
            final char ch = (char) r;
            if (chars.contains(ch)) {
                back(1);
                break;
            }
            b.append(ch);
        }

        return b.toString();
    }

    public final List<Character>
                MATH = Arrays.asList('+', '-', '/', '*', '^'),
                COMPARE = Arrays.asList('<', '>', '!'),
                NUMBERS = Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'),
                SPACES = Arrays.asList(' ', '\t'),
                NEXT_LINE = Arrays.asList('\r', '\n'),

                IMPORT = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '[', ']', '<', '>',

                        '+', '-', '/', '=', '!', '?', ':', '$',

                        ',', ';'
                ),

                S1 = Arrays.asList(' ', '\t', '\r', '\n'),

                E1 = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '[', ']', '<', '>',

                        '\'', '"',

                        '+', '-', '/', '*', '=', '?', ':', '&', '|',

                        '.', '$',

                        ',', ';', '#'
                ),

                E1NoClass = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '[', ']', '<', '>',

                        '\'', '"',

                        '+', '-', '/', '*', '=', '?', ':', '&', '|',

                        ',', ';', '#'
                ),

                E2 = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '[', ']', '<', '>',

                        '+', '-', '/', '*', '=', '?', ':', '$',

                        ',', ';'
                ),

                E3 = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '[', ']',

                        '+', '-', '/', '*', '=', '?', ':', '$',

                        ',', ';'
                ),

                E4 = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}',

                        '+', '-', '/', '*', '=', '?', ':', '$',

                        ',', ';'
                ),

                E5 = Arrays.asList(
                        ' ', '\t', '\r', '\n',

                        '(', ')', '{', '}', '<', '>',

                        '+', '-', '/', '*', '=', '?', ':', '$',

                        ',', ';'
                )
    ;

    @SafeVarargs
    public final boolean orContains(final char ch, final List<Character>... lists) {
        for (final List<Character> l : lists)
            if (l.contains(ch))
                return true;
        return false;
    }

    public I4LParser(final Reader r) { this(r, null); }

    public I4LParser(final Reader r, I4LParserConfig config) {
        this.r = r;

        if (config == null)
            config = new I4LParserConfig();

        if (config.code == null)
            config.code = new ArrayList<>();

        code = config.code;

        if (config.keywords == null)
            config.keywords = I4LParserConfig.BASIC_KEYWORDS;

        keywords = config.keywords;

        try {
            skip(S1);
            while (read() != -1) {
                back(1);
                clear();

                String s = readTo(E1);
                if (r() == '#') {
                    code.add(parsePreprocessor());
                    skip(S1);
                    continue;
                } else
                    back(1);
                if (s.isEmpty()) {
                    if (r() == '/') {
                        final I4LComment c = new I4LComment();
                        char ch = r();

                        if (ch == '/') {
                            c.comment = readTo(Arrays.asList('\r', '\n'));
                            code.add(c);
                            skip(S1);
                            continue;
                        } else if (ch == '*') {
                            c.single = false;
                            final StringBuilder b = new StringBuilder();
                            boolean star = false;
                            while (true) {
                                ch = r();
                                if (star)
                                    if (ch == '/')
                                        break;
                                    else {
                                        b.append('*');
                                        star = false;
                                    }
                                if (ch == '*') {
                                    star = true;
                                    continue;
                                }
                                b.append(ch);
                            }
                            c.comment = b.toString();
                            code.add(c);
                            skip(S1);
                            continue;
                        } else
                            throw new Exception("Unknown combination: /" + ch);
                    } else {
                        back(1);
                        throw new Exception("Empty, next char = " + r());
                    }
                }

                skip(S1);

                if (s.equals("package")) {
                    s = readTo(E2);
                    skip(S1);
                    if (r() == ';') {
                        skip(S1);
                        code.add(new I4LPkg(s));
                        continue;
                    }
                    back(1);
                    throw new Exception("Unexpected in package path " + s + " character: " + r());
                }

                if (s.equals("import")) {
                    s = readTo(IMPORT);
                    skip(S1);
                    if (r() == ';') {
                        skip(S1);
                        code.add(new I4LImport(s));
                        continue;
                    }
                    back(1);
                    throw new Exception("Unexpected in import " + s + " character: " + r());
                }

                final ArrayList<Object> l = new ArrayList<>();
                l.add(s);
                while (true) {
                    s = readTo(E1);
                    if (s.isEmpty()) {
                        char ch = r();
                        if (ch == '(') {
                            final I4LMethod m = new I4LMethod();
                            Object o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Parameter isn't parameter");
                            m.name = (String) o;
                            m.tags = l;

                            parseMethod(m);

                            code.add(m);
                            break;
                        }

                        if (ch == '<') {
                            if (!(l.get(l.size() - 1) instanceof String))
                                throw new Exception("The tag isn't String.");
                            ArrayList<Object> generics = new ArrayList<>();
                            parseGenerics(generics);
                            l.add(new I4LType((String) l.remove(l.size() - 1), generics));
                            skip(S1);
                            ch = r();
                        }

                        if (ch == '{') {
                            System.err.println(l);
                            System.err.println(l.isEmpty());
                            System.err.println(l.size());
                            final I4LClass cls = new I4LClass();
                            cls.tags = l;
                            parseClass(cls);
                            code.add(cls);
                            break;
                        }
                        System.err.println(code);
                        throw new Exception("End " + ch + r() + r() + r() + " - " + l);
                    }
                    l.add(s);
                    skip(S1);
                }
                skip(S1);
            }
        } catch (final Exception ex) {
            this.exception = ex;
        }
    }

    private I4LPreprocessor parsePreprocessor() throws Exception {
        skip(S1);
        final String a = readTo(E1);
        switch (a) {
            case "if":
                return new I4LPIf(parsePreprocessor());
            case "elif":
                back(2);
                return new I4LPElse(parsePreprocessor());
            case "else":
                skip(SPACES);
                if (NEXT_LINE.contains(r()))
                    return new I4LPElse();
                back(1);
                return new I4LPElse(parsePreprocessor());
            case "defined":
                skip(S1);
                if (r() != '(') {
                    back(1);
                    throw new Exception("Unexpected " + r());
                }
                skip(S1);
                final I4LPDefined isDefined = new I4LPDefined(readTo(E1));
                if (r() != ')') {
                    back(1);
                    throw new Exception("Unexpected " + r());
                }
                return isDefined;
            case "end":
                return new I4LPEnd();
            case "error":
                skip(S1);
                if (r() != '"') {
                    back(1);
                    throw new Exception("Unexpected " + r());
                }
                return new I4LPError(parseString().string);
            case "define":
                skip(S1);
                final I4LPDefine def = new I4LPDefine(readTo(S1));
                if (def.name.isEmpty())
                    throw new Exception("Define name is empty");
                skip(SPACES);
                String val = readTo(NEXT_LINE);
                if (val.isEmpty())
                    return def;
                throw new Exception("Unimplemented value " + val);
            default:
                throw new Exception("Unimplemented! " + a);
        }
    }

    private int parseArrayLevels() throws Exception {
        int r = 0;
        char ch;
        while (true) {
            r++;
            skip(S1);
            ch = r();
            if (ch == ']') {
                skip(S1);
                ch = r();
                if (ch == '[')
                    continue;
                back(1);
                return r;
            }
            if (ch == ',')
                continue;
            throw new Exception("Unknown char: " + ch);
        }
    }

    private void parseClass(final I4LClass curCls) throws Exception {
        final ArrayList<Object> code = curCls.code;
        char ch;
        String s;
        while (true) {
            skip(S1);
            ch = r();
            if (ch == '}')
                return;
            back(1);
            final ArrayList<Object> l = new ArrayList<>();
            while (true) {
                s = readTo(E2);
                Object o = s;
                if (s.isEmpty()) {
                    switch (r()) {
                        case ';': {
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Parameter isn't parameter");
                            s = (String) o;
                            l.remove(l.size() - 1);
                            final I4LNewVar nv = new I4LNewVar();
                            nv.params = l;
                            nv.vars.add(new I4LNewVar.Var(s, null));
                            code.add(nv);
                            //System.out.println("NAME: " + s);
                            break;
                        }
                        case '=': {
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Parameter isn't parameter");
                            s = (String) o;
                            l.remove(l.size() - 1);
                            skip(S1);
                            //System.out.println("NAME: " + s);
                            //System.out.println("VALUE: " + parseAdvanced());
                            final I4LNewVar nv = new I4LNewVar();
                            nv.params = l;
                            nv.vars.add(new I4LNewVar.Var(s, parseAdvanced()));
                            code.add(nv);
                            skip(S1);
                            ch = r();
                            if (ch == ';')
                                break;
                            if (ch == ',') {
                                skip(S1);
                                while (true) {
                                    s = readTo(E3);
                                    if (s.isEmpty())
                                        throw new Exception("Name is empty!");
                                    //System.out.println("NAME: " + s);
                                    skip(S1);
                                    ch = r();
                                    if (ch == '=') {
                                        //System.out.println("VALUE: " + parseAdvanced());
                                        nv.vars.add(new I4LNewVar.Var(s, parseAdvanced()));
                                        skip(S1);
                                        ch = r();
                                    } else
                                        nv.vars.add(new I4LNewVar.Var(s, null));
                                    if (ch == ';')
                                        break;
                                    if (ch == ',') {
                                        skip(S1);
                                        continue;
                                    }
                                    throw new Exception("Unexpected " + ch);
                                }
                                break;
                            }
                            throw new Exception("Unexpected " + ch);
                        }
                        case ',': {
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Parameter isn't parameter");
                            s = (String) o;
                            l.remove(l.size() - 1);
                            skip(S1);
                            //System.out.println("NAME: " + s);
                            final I4LNewVar nv = new I4LNewVar();
                            nv.params = l;
                            nv.vars.add(new I4LNewVar.Var(s, null));
                            code.add(nv);
                            while (true) {
                                s = readTo(E3);
                                if (s.isEmpty())
                                    throw new Exception("Name is empty!");
                                //System.out.println("NAME: " + s);
                                skip(S1);
                                ch = r();
                                if (ch == '=') {
                                    //System.out.println("VALUE: " + parseAdvanced());
                                    nv.vars.add(new I4LNewVar.Var(s, parseAdvanced()));
                                    skip(S1);
                                    ch = r();
                                } else
                                    nv.vars.add(new I4LNewVar.Var(s, null));
                                if (ch == ';')
                                    break;
                                if (ch == ',') {
                                    skip(S1);
                                    continue;
                                }
                                if (ch == '}')
                                    if (curCls.tags.contains("enum") && nv.params.isEmpty())
                                        return;
                                System.out.println(curCls.tags);
                                System.out.println(nv.params);
                                System.out.println(nv.vars);
                                throw new Exception("Unexpected " + ch);
                            }
                            break;
                        }
                        case '(': {
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Expected parameter name as a String, but found: " + o.getClass().getSimpleName());
                            s = (String) o; // Take the name
                            l.remove(l.size() - 1);

                            if (s.startsWith("@")) { // Annotation
                                final I4LCallAnnotation annotation = new I4LCallAnnotation(s);
                                parseCallAnnotation(annotation);
                                code.add(annotation);
                                skip(S1);
                                continue;
                            }

                            final I4LMethod method = new I4LMethod();
                            method.name = s;
                            method.tags = l;

                            parseMethod(method);

                            code.add(method);
                            break;
                        }
                        case '{':
                            final I4LClass cls = new I4LClass();
                            cls.tags = l;
                            parseClass(cls);
                            code.add(cls);
                            break;
                        case '/':
                            final I4LComment c = new I4LComment();
                            ch = r();

                            if (ch == '/') {
                                c.comment = readTo(Arrays.asList('\r', '\n'));
                                code.add(c);
                                break;
                            } else if (ch == '*') {
                                c.single = false;
                                final StringBuilder b = new StringBuilder();
                                boolean star = false;
                                while (true) {
                                    ch = r();
                                    if (star)
                                        if (ch == '/')
                                            break;
                                        else {
                                            b.append('*');
                                            star = false;
                                        }
                                    if (ch == '*') {
                                        star = true;
                                        continue;
                                    }
                                    b.append(ch);
                                }
                                c.comment = b.toString();
                                code.add(c);
                                break;
                            } else
                                back(1);
                        case '[':
                            int lvl = 1;
                            skip(S1);
                            ch = r();
                            while (true) {
                                if (ch == ']') {
                                    skip(S1);
                                    ch = r();
                                    if (ch == '[') {
                                        lvl++;
                                        skip(S1);
                                        ch = r();
                                        continue;
                                    }
                                    back(1);
                                    break;
                                }
                                if (ch == ',') {
                                    lvl++;
                                    skip(S1);
                                    ch = r();
                                    continue;
                                }
                                throw new Exception("Unknown char: " + ch);
                            }
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Expected parameter name as a String, but found: " + o.getClass().getSimpleName());
                            s = (String) o;
                            l.remove(l.size() - 1);
                            code.add(new I4LArr(s, lvl));
                            skip(S1);
                            continue;
                        case '<':
                            o = l.get(l.size() - 1);
                            if (!(o instanceof String))
                                throw new Exception("Expected parameter name as a String, but found: " + o.getClass().getSimpleName());
                            ArrayList<Object> generics = new ArrayList<>();
                            parseGenerics(generics);
                            l.add(new I4LType((String) l.remove(l.size() - 1), generics));
                            skip(S1);
                            continue;
                        default:
                            back(1);
                            throw new Exception("Unexpected " + r() + ", " + l);
                    }
                    break;
                }
                l.add(o);
                skip(S1);
            }
            //throw new Exception("Unexpected " + ch);
        }
    }

    private int parseGenerics(final List<Object> generics) throws Exception {
        int c = skip(S1);
        char ch = r();
        if (ch == '>')
            return ++c;
        back(1);
        while (true) {
            final String generic = readTo(E1);
            if (generic.isEmpty())
                throw new Exception("Generic name can't be empty");
            c += skip(S1) + 1 + generic.length();
            ch = r();
            if (ch == '<') {
                final ArrayList<Object> subGenerics = new ArrayList<>();
                parseGenerics(subGenerics);
                generics.add(new I4LType(generic, subGenerics));
                c += skip(S1) + 1;
                ch = r();
            } else
                generics.add(generic);
            if (ch == '>')
                return c;
            if (ch == ',') {
                c += skip(S1);
                continue;
            }
            throw new Exception("Unknown char: " + ch);
        }
    }

    private void parseMethod(final I4LMethod method) throws Exception {
        skip(S1);
        char ch = r();
        if (ch != ')') {
            if (ch == ',')
                throw new Exception("Unexpected " + ch);
            back(1);
            I4LArg a = new I4LArg();
            String p;
            while (true) {
                p = readTo(E5);
                if (p.isEmpty())
                    throw new Exception("Empty parameter!");
                a.params.add(p);
                skip(S1);
                ch = r();
                if (ch == '<') {
                    final StringBuilder b = new StringBuilder(p);
                    a.params.remove(a.params.size() - 1);
                    b.append('<');
                    while ((ch = r()) != '>')
                        b.append(ch);
                    b.append('>');
                    a.params.add(b.toString());
                    skip(S1);
                    ch = r();
                }
                if (ch == ')') {
                    method.args.add(a);
                    break;
                }
                if (ch == ',') {
                    skip(S1);
                    method.args.add(a);
                    a = new I4LArg();
                    continue;
                }
                back(1);
            }
        }

        skip(S1);
        ch = r();

        if (ch == 't') {
            String t = readTo(E1);
            if (!t.equals("hrows"))
                throw new Exception("Unexpected " + ch + t);
            while (true) {
                skip(S1);
                t = readTo(E4);
                if (t.isEmpty())
                    throw new Exception("Empty Exception path!");
                method.exceptions.add(t);
                skip(S1);
                ch = r();
                if (ch == ',')
                    continue;
                if (ch == '{' || ch == ';')
                    break;
                throw new Exception("Unexpected " + ch);
            }
        }

        if (ch == ';')
            return;

        if (ch != '{')
            throw new Exception("Unexpected " + ch);
        parseCode(method.code);
    }

    private void parseCallAnnotation(final I4LCallAnnotation annotation) throws Exception {
        char ch;
        final ArrayList<Object> parameters = new ArrayList<>();
        annotation.args = parameters;
        skip(S1);
        ch = r();
        if (ch == ')')
            return;
        back(1);
        while (true) {
            parseAnnotationArgument(parameters);
            skip(S1);
            ch = r();
            if (ch == ')')
                return;
            if (ch == ',')
                continue;
            throw new Exception("Unknown char: " + ch);
        }
    }

    private void parseAnnotationArgument(final ArrayList<Object> parameters) throws Exception {
        skip(S1);
        char ch = r();
        if (ch == '{') {
            final ArrayList<Object> arr = new ArrayList<>();
            parameters.add(arr);
            skip(S1);
            ch = r();
            if (ch == '}')
                return;
            back(1);
            while (true) {
                parseAnnotationArgument(arr);
                skip(S1);
                ch = r();
                if (ch == '}')
                    return;
                if (ch == ',')
                    continue;
                throw new Exception("Unknown char: " + ch);
            }
        }
        if (ch == '"') {
            parameters.add(parseString());
            return;
        }
        throw new Exception("Unknown char: " + ch);
    }

    public char backslashChar(final char ch) throws Exception {
        switch (ch) {
            case 'n': return '\n';
            case 'r': return '\r';
            case 'b': return '\b';
            case 't': return '\t';
            case 's': return ' ';
            case '"': return '"';
            case '\'': return '\'';
            case '\\': return '\\';
            default: throw new Exception("Unexpected " + ch);
        }
    }

    public char parseChar() throws Exception {
        char ch = r();
        if (ch == '\\')
            ch = backslashChar(r());
        final char ch2 = r();
        if (ch2 != '\'')
            throw new Exception("Unexpected " + ch2);
        return ch;
    }

    public I4LString parseString() throws Exception {
        final StringBuilder b = new StringBuilder();
        char ch;
        while (true) {
            ch = r();
            if (ch == '\\') {
                ch = r();
                if (ch == 'u') {
                    final StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < 4; i++) {
                        ch = r();
                        if (Character.digit(ch, 16) == -1)
                            throw new Exception("Invalid Unicode escape sequence: \\u" + hex + ch);
                        hex.append(ch);
                    }
                    int codePoint = Integer.parseInt(hex.toString(), 16);
                    b.append((char) codePoint);
                    continue;
                }
                b.append(backslashChar(ch));
                continue;
            }
            if (ch == '"')
                break;
            b.append(ch);
        }
        return new I4LString(b.toString());
    }

    public Object parseNumber() throws Exception {
        final StringBuilder b = new StringBuilder();
        boolean dot = false;
        char ch;
        while (true) {
            ch = r();
            if (
                    ch == '0' ||
                    ch == '1' ||
                    ch == '2' ||
                    ch == '3' ||
                    ch == '4' ||
                    ch == '5' ||
                    ch == '6' ||
                    ch == '7' ||
                    ch == '8' ||
                    ch == '9' ||
                    ch == '.'
            ) {
                if (ch == '.') {
                    if (dot)
                        throw new Exception("Double dots in the number");
                    dot = true;
                }
                b.append(ch);
                continue;
            }
            if (ch == 'f')
                return Float.parseFloat(b.toString());
            back(1);
            break;
        }
        return Integer.parseInt(b.toString());
    }

    public I4LOperation parseOperation() throws Exception { return parseOperation(true); }
    public I4LOperation parseOperation(final boolean multiLined) throws Exception {
        skip(S1);
        char ch = r();
        if (ch == '}' || ch == ')' || ch == ';' || ch == ',' || ch == ':' || ch == '|' || ch == '&') {
            back(1);
            //System.out.println("RETURN NULL, CHAR = " + ch);
            return null;
        }
        if (ch == '/') {
            final I4LComment c = new I4LComment();
            ch = r();

            if (ch == '/')
                c.comment = readTo(Arrays.asList('\r', '\n'));
            else if (ch == '*') {
                c.single = false;
                final StringBuilder b = new StringBuilder();
                boolean s = false;
                while (true) {
                    ch = r();
                    if (s)
                        if (ch == '/')
                            break;
                        else {
                            b.append('*');
                            s = false;
                        }
                    if (ch == '*') {
                        s = true;
                        continue;
                    }
                    b.append(ch);
                }
                c.comment = b.toString();
            } else
                throw new Exception("Unexpected " + ch);
            return c;
        }
        if (ch == '(') {
            back(1);
            final Object o = parseAdvanced();
            if (o instanceof I4LOperation)
                return (I4LOperation) o;
            throw new Exception("It's not an operation " + o.getClass().getName() + " : " + o);
        }
        if (ch == '{') {
            final I4LCode c = new I4LCode();
            parseCode(c.code);
            return c;
        }
        if (ch == '#')
            return parsePreprocessor();
        if (E1.contains(ch))
            throw new Exception("Unexpected " + ch);
        back(1);
        String t = readTo(E3);
        if (t.equals("case")) {
            final I4LCase c = new I4LCase();
            c.value = parseAdvanced();
            skip(S1);
            ch = r();
            if (ch != ':')
                throw new Exception("Unexpected " + ch + ", expected ':' after case");
            return c;
        }
        if (keywords.contains(t)) {
            final I4LKeyword k = new I4LKeyword();
            k.name = t;
            k.operation = parseAdvanced();
            return k;
        }

        int skipped = skip(S1) + 1;
        ch = r();
        if (!t.isEmpty())
            switch (ch) {
                case ':':
                    if (!multiLined) {
                        back(1);
                        return new I4LGet(t);
                    }
                    final I4LMark m = new I4LMark();
                    m.name = t;
                    return m;
                case '(': {
                    final List<Object> l = new ArrayList<>();
                    skip(S1);
                    ch = r();
                    if (ch != ')') {
                        back(1);
                        while (true) {
                            final Object o = parseAdvanced();
                            l.add(o);
                            skip(S1);
                            ch = r();
                            if (ch == ',' || ch == ';')
                                continue;
                            if (ch == ')')
                                break;
                            if (ch == ':' && l.size() == 1) {
                                final I4LForEach fe = new I4LForEach();
                                fe.params = new ArrayList<>();
                                Object target = l.get(0);
                                while (true) {
                                    if (target instanceof I4LKeyword) {
                                        fe.params.add(((I4LKeyword) target).name);
                                        target = ((I4LKeyword) target).operation;
                                        continue;
                                    }
                                    if (target instanceof I4LNewVar) {
                                        if (((I4LNewVar) target).vars.size() != 1)
                                            throw new Exception("Can't be used for forEach: " + target);
                                        fe.params.addAll(((I4LNewVar) target).params);
                                        final I4LNewVar.Var v = ((I4LNewVar) target).vars.get(0);
                                        if (v.value != null)
                                            throw new Exception("It can't have any values! " + v);
                                        fe.params.add(v.name);
                                        break;
                                    }
                                    throw new Exception("Unknown class " + target.getClass() + " for forEach!");
                                }
                                fe.of = parseAdvanced();
                                skip(S1);
                                ch = r();
                                if (ch != ')')
                                    throw new Exception("Unknown char: " + ch);
                                return fe;
                            }
                            throw new Exception("Unexpected " + t + " / " + l + " / " + ch);
                        }
                    }
                    skip(S1);
                    ch = r();
                    if (MATH.contains(ch) || COMPARE.contains(ch) || ch == ';' || ch == '=' || ch == ')' || ch == ',' || ch == '|' || ch == '&' || ch == '?' || ch == ':') {
                        back(1);
                        final I4LCall c = new I4LCall();
                        c.target = t;
                        c.args = l;
                        return c;
                    }
                    if (ch == '{') {
                        final I4LStatement s = new I4LStatement();
                        s.name = t;
                        s.begin = l;
                        s.code = new ArrayList<>();
                        parseCode(s.code);
                        if (t.equals("if")) {
                            skip(S1);
                            final String ek = readTo(E1);
                            if (ek.equals("else")) {
                                final I4LIfElse ie = new I4LIfElse(s);
                                skip(S1);
                                ch = r();
                                if (ch != '{') {
                                    back(1);
                                    final Object o = parseAdvanced();
                                    if (o == null)
                                        throw new Exception("Else operation is null!");
                                    ie.elseCode.add(o);
                                } else
                                    parseCode(ie.elseCode);
                                return ie;
                            }
                            back(ek.length());
                        }
                        return s;
                    }
                    if (ch == '.') {
                        final I4LSub s = new I4LSub();

                        final I4LCall c = new I4LCall();
                        c.target = t;
                        c.args = l;

                        s.object = c;
                        s.path = parseValue();

                        return s;
                    }
                    if (E1.contains(ch) && ch != '(')
                        throw new Exception("Unexpected " + ch + ", t = " + t);
                    back(1);
                    final I4LStatement s = new I4LStatement();
                    s.name = t;
                    s.begin = l;
                    s.sub = false;
                    s.code = new ArrayList<>();
                    Object o = parseAdvanced();
                    if (o == null)
                        throw new Exception("NULL");
                    s.code.add(o);
                    if (r() != ';')
                        back(1);
                    if (t.equals("if")) {
                        skip(S1);
                        final String ek = readTo(E1);
                        if (ek.equals("else")) {
                            final I4LIfElse ie = new I4LIfElse(s);
                            skip(S1);
                            ch = r();
                            if (ch != '{') {
                                back(1);
                                o = parseAdvanced();
                                if (o == null)
                                    throw new Exception("Else operation is null!");
                                ie.elseCode.add(o);
                            } else
                                parseCode(ie.elseCode);
                            return ie;
                        }
                        back(ek.length());
                    }
                    return s;
                }
                case ';':
                    final I4LKeyword kw = new I4LKeyword();
                    kw.name = t;
                    return kw;
                case '=':
                    final I4LSet set = new I4LSet();
                    set.path = t;
                    set.operation = '=';
                    set.value = parseAdvanced();
                    return set;
                case '{':
                    final I4LStatement statement = new I4LStatement();
                    statement.name = t;
                    statement.code = new ArrayList<>();
                    parseCode(statement.code);
                    if (t.equals("if")) {
                        skip(S1);
                        final String ek = readTo(E1);
                        if (ek.equals("else")) {
                            final I4LIfElse ie = new I4LIfElse(statement);
                            skip(S1);
                            ch = r();
                            if (ch != '{') {
                                back(1);
                                final Object o = parseAdvanced();
                                if (o == null)
                                    throw new Exception("Else operation is null!");
                                ie.elseCode.add(o);
                            } else
                                parseCode(ie.elseCode);
                            return ie;
                        }
                        back(ek.length());
                    }
                    return statement;
            }
        if (MATH.contains(ch)) {
            final char ch2 = r();
            if (ch != ch2)
                throw new Exception("Unexpected " + ch + ch2);
            final I4LSetAfter s = new I4LSetAfter();
            s.path = t;
            s.operation = ch;
            return s;
        }
        if (E1.contains(ch)) {
            if (t.isEmpty())
                throw new Exception("Unexpected " + ch + " after " + t);
            if (ch == '<' && r() == '<') {
                final I4LMove m = new I4LMove();
                m.target = new I4LGet(t);
                m.action = "<<";
                m.steps = parseAdvanced();
                return m;
            } else
                back(2);
            final I4LKeyword k = new I4LKeyword();
            k.name = t;
            k.operation = parseAdvanced();
            return k;
        }
        back(1);
        String t2 = readTo(E1);
        int s = skip(S1);
        if (t2.equals("instanceof")) {
            final I4LInstanceOf iof = new I4LInstanceOf();
            iof.object = new I4LGet(t);
            iof.cn = readTo(E4);
            return iof;
        }
        ch = r();
        if (ch == ';' || ch == '(' || MATH.contains(ch) || !E1.contains(ch)) {
            final I4LKeyword k = new I4LKeyword();
            k.name = t;
            back(1 + s + t2.length());
            k.operation = parseAdvanced();
            return k;
        }
        if (ch == '<' || ch == '[') {
            final I4LKeyword k = new I4LKeyword();
            k.name = t;
            back(1 + s + t2.length());
            k.operation = parseAdvanced();
            return k;
        }
        if (ch == '=') {
            ch = r();
            if (ch == '=') {
                final I4LCompare c = new I4LCompare();
                c.check = "==";

                //return c;
                throw new Exception(t + " / " + t2);
            }
            back(2);
            ch = r();
            final I4LNewVar nv = new I4LNewVar();
            nv.params = new ArrayList<>();
            nv.params.add(t);
            nv.vars = new ArrayList<>();
            while (true) {
                if (ch == '=') {
                    nv.vars.add(new I4LNewVar.Var(t2, parseAdvanced()));
                    skip(S1);
                    ch = r();
                } else
                    nv.vars.add(new I4LNewVar.Var(t2, null));
                if (ch == ',') {
                    skip(S1);
                    t2 = readTo(E4);
                    skip(S1);
                    ch = r();
                    continue;
                }
                if (ch == ';' || ch == ')') {
                    back(1);
                    return nv;
                }
                System.out.println(nv);
                throw new Exception("Unexpected " + ch + ", t2 = " + t2 + ", t = " + t);
            }
            //throw new Exception("SET " + t + " " + t2 + " = " + parse());
        }
        if (ch == ':') {
            final I4LForEach f = new I4LForEach();
            f.params = new ArrayList<>();
            f.params.add(t);
            f.params.add(t2);
            f.of = parseAdvanced();
            return f;
        }
        if (ch == ')') {
            final I4LNewVar nv = new I4LNewVar();
            nv.params = new ArrayList<>();
            nv.params.add(t);
            nv.vars = new ArrayList<>();
            nv.vars.add(new I4LNewVar.Var(t2, null));
            back(1);
            return nv;
        }
        throw new Exception("Unexpected " + t + " " + t2 + " " + ch);
    }

    public void parseCode(final List<Object> code) throws Exception {
        while (true) {
            final int bl = line;
            final I4LOperation o = parseOperation(true);
            System.out.println(o);
            if (o == null) {
                final char ch = r();
                if (ch == '}')
                    break;
                if (ch == ';')
                    continue;
                throw new Exception("Null, next char = " + ch);
            }
            code.add(o);
            //System.out.println("[CODE:" + bl + " - " + line + "] " + o);
        }
    }

    public Object parseAdvanced() throws Exception {
        char ch;
        Object r = null;
        while (true) {
            final Object o = parseValue();
            if (o == null)
                throw new Exception("NULL");
            if (r == null)
                r = o;
            else if (o instanceof I4LEmpty) {
                if (
                        (!(r instanceof I4LSet) || ((I4LSet) r).value == null) &&
                                (!(r instanceof I4LSetAfter))
                ) {
                    if (r instanceof I4LCompare && ((I4LCompare) r).v1 instanceof I4LGet) {
                        if (r() == '>') {
                            r = new I4LType(((I4LGet) ((I4LCompare) r).v1).path);
                        } else
                            throw new Exception("Mixing with empty! " + r + r());
                    } else
                        throw new Exception("Mixing with empty! " + r);
                }
            } else if (r instanceof I4LCombine) {
                if (o instanceof I4LGet)
                    if (((I4LGet) o).path.isEmpty())
                        throw new Exception("GET is empty!" + o);
                ((I4LCombine) r).v2 = o;
            } else if (r instanceof I4LCompare)
                ((I4LCompare) r).v2 = o;
            else if (r instanceof I4LSet)
                ((I4LSet) r).value = o;
            else if (r instanceof I4LSub)
                ((I4LSub) r).path = o;
            else if (r instanceof I4LSetAfter && ((I4LSetAfter) r).path instanceof I4LEmpty) {
                final I4LSet s = new I4LSet();
                s.path = o;
                s.operation = ((I4LSetAfter) r).operation;
                s.value = 1;
                r = s;
            } else if (r instanceof I4LLambda && (o instanceof I4LOperation))
                ((I4LLambda) r).operation = o;
            else if (r instanceof I4LType && o instanceof I4LArrType)
                ((I4LType) r).arrayLevels = ((I4LArrType) o).arrayLevels;
            else if (r instanceof I4LType && o instanceof I4LGet) {
                final I4LNewVar nv = new I4LNewVar();
                nv.params = new ArrayList<>();
                nv.params.add(r);
                nv.vars.add(new I4LNewVar.Var(((I4LGet) o).path, null));
                r = nv;
            } else
                throw new Exception("Unimplemented mixing of the " + r.getClass() + " and " + o.getClass() + " / " + r + " : " + o);
            if (o instanceof I4LStatement && ((I4LStatement) o).sub)
                return r;
            skip(S1);
            ch = r();
            if (ch == ';' || ch == ':' || ch == ',' || ch == ')') {
                if (ch == ',' && r instanceof I4LCompare && ((I4LCompare) r).check.equals("<")) {

                    throw new Exception("est " + r);
                }
                back(1);
                return r;
            }
            if (ch == '?') {
                final I4LCodeIf c = new I4LCodeIf();
                c.check = r;
                c.then = parseAdvanced();
                skip(S1);
                ch = r();
                if (ch != ':')
                    throw new Exception("Unexpected " + ch);
                c.el = parseAdvanced();
                return c;
            }
            if (MATH.contains(ch)) {
                if (ch == '-') {
                    if (r() == '>') {
                        if (r instanceof I4LGet) {
                            final I4LLambda lambda = new I4LLambda(new ArrayList<>(Collections.singleton(new I4LLambda.Argument(((I4LGet) r).path))));
                            //lambda.operation = parseAdvanced();
                            r = lambda;
                            continue;
                        }
                        throw new Exception("Lambda with the start isn't supported: " + r);
                    }
                    back(1);
                }
                final I4LCombine c = new I4LCombine(ch);
                ch = r();
                if (ch == c.operation) {
                    final I4LSetAfter s = new I4LSetAfter();
                    s.operation = ch;
                    s.path = r;
                    r = s;
                    continue;
                } else
                    back(1);
                c.v1 = r;
                r = c;
                continue;
            }
            if (ch == '=') {
                ch = r();
                if (ch != '=') {
                    back(1);
                    final I4LSet s = new I4LSet();
                    s.operation = '=';
                    s.path = r;
                    r = s;
                    continue;
                }
                final I4LCompare c = new I4LCompare();
                c.check = "==";
                c.v1 = r;
                r = c;
                continue;
            }
            if (COMPARE.contains(ch)) {
                final StringBuilder b = new StringBuilder();
                b.append(ch);
                ch = r();
                if (ch == '=')
                    b.append('=');
                else
                    back(1);
                if (r instanceof I4LCompare && ((I4LCompare) r).check.equals("<") && ((I4LCompare) r).v1 instanceof I4LGet && ((I4LCompare) r).v2 instanceof I4LGet && b.toString().equals(">")) {
                    final ArrayList<Object> generics = new ArrayList<>();
                    generics.add(((I4LGet) ((I4LCompare) r).v2).path);
                    r = new I4LType(((I4LGet) ((I4LCompare) r).v1).path, generics);
                    continue;
                }
                final I4LCompare c = new I4LCompare();
                System.out.println(r + " + " + b);
                c.check = b.toString();
                c.v1 = r;
                r = c;
                continue;
            }
            if (ch == '|' || ch == '&') {
                final char ch2 = r();
                if (ch2 != ch)
                    throw new Exception("Unexpected " + ch + ch2);
                final I4LCompare c = new I4LCompare();
                c.check = "" + ch + ch;
                c.v1 = r;
                r = c;
                continue;
            }
            if (o instanceof I4LSubArg && ((I4LSubArg) o).value instanceof I4LGet) {
                back(1);
                final I4LConvert c = new I4LConvert();
                c.to = ((I4LGet) ((I4LSubArg) o).value).path;
                c.value = parseAdvanced();
                return c;
            }
            if (o instanceof I4LSubArg && ((I4LSubArg) o).value instanceof I4LType) {
                back(1);
                final I4LConvert c = new I4LConvert();
                c.to = ((I4LSubArg) o).value;
                c.value = parseAdvanced();
                return c;
            }
            if (ch == '.') {
                final I4LSub s = new I4LSub();
                s.object = r;
                r = s;
                continue;
            }
            if (r instanceof I4LType && o instanceof I4LEmpty && ch == '(') {
                final List<Object> l = new ArrayList<>();
                skip(S1);
                ch = r();
                if (ch != ')') {
                    back(1);
                    while (true) {
                        l.add(parseAdvanced());
                        skip(S1);
                        ch = r();
                        if (ch == ',')
                            continue;
                        if (ch == ')')
                            return new I4LCall(r, l);
                        throw new Exception("Unexpected " + ch);
                    }
                }
                return new I4LCall(r, l);
            }
            if (r instanceof I4LType && ((I4LType) r).arrayLevels > 0) {
                back(1);
                final I4LNewVar nv = new I4LNewVar();
                nv.params = new ArrayList<>();
                nv.params.add(r);
                System.out.println(ch + " / " + nv);
                while (true) {
                    final String varName = readTo(E1);
                    if (varName.isEmpty())
                        throw new Exception("Var name can't be empty!");
                    skip(S1);
                    ch = r();
                    if (ch == '=') {
                        nv.vars.add(new I4LNewVar.Var(varName, parseAdvanced()));
                        skip(S1);
                        ch = r();
                        if (ch == ',')
                            continue;
                        else if (ch == ';')
                            return nv;
                        throw new Exception("Unknown char: " + ch);
                    } else if (ch == ',') {
                        nv.vars.add(new I4LNewVar.Var(varName, null));
                        continue;
                    } else if (ch == ';') {
                        nv.vars.add(new I4LNewVar.Var(varName, null));
                        return nv;
                    }
                    throw new Exception("Unknown char: " + ch);
                }
            }
            //throw new Exception("Unknown char: " + ch + " / " + r + " / " + o);
            back(1);
            return o;
        }
    }

    public Object parseValue() throws Exception {
        skip(S1);
        switch (r()) {
            case '\'': return parseChar();
            case '"': return parseString();
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '.':
                back(1);
                return parseNumber();
            case '(': {
                final I4LSubArg sub = new I4LSubArg();
                sub.value = parseAdvanced();
                skip(S1);
                final char ch2 = r();
                if (ch2 != ')')
                    throw new Exception("Unexpected " + ch2);
                if (sub.value instanceof I4LEmpty) {
                    skip(S1);
                    if (r() == '-')
                        if (r() == '>') {
                            final I4LLambda l = new I4LLambda(new ArrayList<>());
                            l.operation = parseAdvanced();
                            return l;
                        } else
                            back(2);
                    else
                        back(1);
                    throw new Exception("Unknown char: " + r());
                }
                return sub;
            }
            case '-': {
                final char ch2 = r();
                back(1);
                if (NUMBERS.contains(ch2) || ch2 == '.')
                    return new I4LNegative(parseNumber());
                break;
            }
            case 't':
                if (r() == 'r')
                    if (r() == 'u')
                        if (r() == 'e')
                            if (orContains(r(), S1, MATH)) {
                                back(1);
                                return true;
                            } else
                                back(4);
                        else
                            back(3);
                    else
                        back(2);
                else
                    back(1);
                break;
            case 'n':
                if (r() == 'u')
                    if (r() == 'l')
                        if (r() == 'l')
                            if (orContains(r(), S1, MATH)) {
                                back(1);
                                return new I4LNull();
                            } else
                                back(4);
                        else
                            back(3);
                    else
                        back(2);
                else
                    back(1);
                break;
        }
        back(1);
        final String n = readTo(E1NoClass);
        final int s = skip(S1);
        char ch = r();
        if (ch == ';' || ch == ')' || ch == ',' || MATH.contains(ch) || COMPARE.contains(ch) || ch == '=' || ch == '?' || ch == ']') {
            back(1);
            if (n.isEmpty())
                return new I4LEmpty();
            return new I4LGet(n);
        }
        if (n.isEmpty() && ch == '[')
            return new I4LArrType(parseArrayLevels());
        if (!n.isEmpty() && ch == '[') {
            skip(S1);
            ch = r();
            if (ch == ']') {
                back(1);
                return new I4LType(n, null, parseArrayLevels());
            }
            back(1);
            I4LSubArr r = new I4LSubArr(new I4LGet(n), parseValue());
            while (true) {
                skip(S1);
                ch = r();
                if (ch == ',') {
                    r = new I4LSubArr(r, parseValue());
                    continue;
                } else if (ch == ']') {
                    skip(S1);
                    if (r() == '[') {
                        r = new I4LSubArr(r, parseValue());
                        continue;
                    }
                    back(1);
                    return r;
                }
                throw new Exception("Unknown character: " + ch);
            }
        }
        System.out.println("Parse: " + n + ch);
        back(n.length() + s + 1);
        final I4LOperation o = parseOperation(false);
        if (o == null)
            throw new Exception("NULL");
        return o;
    }
}