package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Utils.MiniUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;

public class StyleProperty {
    public final String name;
    public final ArrayList<ArrayList<Object>> objs = new ArrayList<>();

    public StyleProperty(final String name) {
        this.name = Objects.requireNonNull(name).toLowerCase();
    }

    public static final int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

    public static String hex2str(final String s) {
        final int c = Integer.parseInt(s, 16);
        return Character.isValidCodePoint(c) ? new String(Character.toChars(c)) : "\uFFFD";
    }

    public static StyleProperty parse(final String name, final String value) {
        final StyleProperty v = new StyleProperty(name);
        final Stack<ArrayList<ArrayList<Object>>> stack = new Stack<>();
        final Stack<ArrayList<Object>> stackSet = new Stack<>();
        ArrayList<ArrayList<Object>> gs = v.objs;
        ArrayList<Object> set = new ArrayList<>();
        final StringBuilder b = new StringBuilder(), s = new StringBuilder();
        byte m = 0;
        boolean special = false;
        m:
        for (final char ch : value.toCharArray())
            switch (m) {
                case 0:
                    switch (ch) {
                        case ' ':
                            if (b.length() == 0)
                                continue;
                            set.add(b.toString());
                            b.setLength(0);
                            continue;
                        case ',':
                            if (b.length() > 0) {
                                set.add(b.toString());
                                b.setLength(0);
                            }
                            if (!set.isEmpty())
                                gs.add(set);
                            set = new ArrayList<>();
                            continue;
                        case '(':
                            final StyleCall c = new StyleCall(b.toString().toLowerCase());
                            b.setLength(0);
                            set.add(c);
                            stack.push(gs);
                            stackSet.push(set);
                            set = new ArrayList<>();
                            gs = c.objs;
                            continue;
                        case ')':
                            if (b.length() > 0) {
                                set.add(b.toString());
                                b.setLength(0);
                            }
                            if (!set.isEmpty())
                                gs.add(set);
                            gs = stack.pop();
                            set = stackSet.pop();
                            if (gs == null) {
                                gs = v.objs;
                                set = new ArrayList<>();
                                break m;
                            }
                            continue;
                        case '\'':
                            if (b.length() > 0) {
                                set.add(b.toString());
                                b.setLength(0);
                            }
                            m = 2;
                            continue;
                        case '"':
                            if (b.length() > 0) {
                                set.add(b.toString());
                                b.setLength(0);
                            }
                            m = 3;
                            continue;
                        case '\\':
                            m = 1;
                            continue;
                        case '+':
                        case '-':
                        case '*':
                        case '/':
                            if (b.length() > 0) {
                                set.add(b.toString());
                                b.setLength(0);
                            }
                            set.add(Character.toString(ch));
                            continue;
                        default:
                            b.append(ch);
                            continue;
                    }
                case 1:
                    b.append(ch);
                    continue;
                case 2:
                    switch (ch) {
                        case '\\':
                            if (special) {
                                if (s.length() == 0) {
                                    special = false;
                                    b.append('\\');
                                    continue;
                                } else {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                }
                                continue;
                            }
                            special = true;
                            continue;
                        case '\'':
                            if (special) {
                                special = false;
                                if (s.length() == 0) {
                                    b.append('\'');
                                    continue;
                                } else {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                }
                            }
                            set.add(new StyleStr(b.toString()));
                            b.setLength(0);
                            m = 0;
                            continue;
                        default:
                            if (special) {
                                if (
                                        (ch >= '0' && ch <= '9') ||
                                        (ch >= 'a' && ch <= 'f') ||
                                        (ch >= 'A' && ch <= 'F')
                                ) {
                                    s.append(ch);
                                    if (s.length() == 6) {
                                        b.append(hex2str(s.toString()));
                                        s.setLength(0);
                                        special = false;
                                    }
                                    continue;
                                } else if (s.length() > 0) {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                    special = false;
                                }
                            }
                            b.append(ch);
                            continue;
                    }
                case 3:
                    switch (ch) {
                        case '\\':
                            if (special) {
                                if (s.length() == 0) {
                                    special = false;
                                    b.append('\\');
                                    continue;
                                } else {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                }
                                continue;
                            }
                            special = true;
                            continue;
                        case '"':
                            if (special) {
                                special = false;
                                if (s.length() == 0) {
                                    b.append('"');
                                    continue;
                                } else {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                }
                            }
                            set.add(new StyleStr(b.toString()));
                            b.setLength(0);
                            m = 0;
                            continue;
                        default:
                            if (special) {
                                if (
                                        (ch >= '0' && ch <= '9') ||
                                        (ch >= 'a' && ch <= 'f') ||
                                        (ch >= 'A' && ch <= 'F')
                                ) {
                                    s.append(ch);
                                    if (s.length() == 6) {
                                        b.append(hex2str(s.toString()));
                                        s.setLength(0);
                                        special = false;
                                    }
                                    continue;
                                } else if (s.length() > 0) {
                                    b.append(hex2str(s.toString()));
                                    s.setLength(0);
                                    special = false;
                                }
                            }
                            b.append(ch);
                            continue;
                    }
            }
        if (b.length() > 0)
            set.add(b.toString());
        if (!set.isEmpty())
            gs.add(set);
        return v;
    }

    private static final ThreadLocal<ArrayList<Object>> tr = ThreadLocal.withInitial(ArrayList::new);

    public static final Predicate<Object> varFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "var".equals(c.name);
        }
        return false;
    }, numberFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "calc".equals(c.name);
        } else if (obj instanceof String) {
            String n = ((String) obj).toLowerCase();
            if (n.endsWith("deg"))
                n = n.substring(0, n.length() - 3);
            else if (n.endsWith("px") || n.endsWith("dp") || n.endsWith("sp"))
                n = n.substring(0, n.length() - 2);
            else if (n.endsWith("%"))
                n = n.substring(0, n.length() - 1);
            if (n.isEmpty())
                return false;
            boolean fd = false;
            for (final char ch : n.toCharArray())
                if (!Character.isDigit(ch))
                    if (ch != '.' || fd)
                        return false;
                    else
                        fd = true;
            return true;
        }
        return obj instanceof Float || obj instanceof Integer;
    }, paintFilter = obj -> {
        if (obj instanceof String) {
            final String s = (String) obj;
            try {
                if (s.startsWith("#") || s.startsWith("0x"))
                    return true;
            } catch (final Exception ignored) {}
            final Color co = Color.getConstant(s);
            return co != null;
        }
        return false;
    }, imageFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "url".equals(c.name);
        }
        return false;
    };

    public static String getVarName(final Object o) {
        if (!(o instanceof StyleCall))
            return null;
        final StyleCall c = (StyleCall) o;
        if (!"var".equals(c.name) || c.objs.isEmpty() || c.objs.get(0).isEmpty())
            return null;
        final Object n = c.objs.get(0).get(0);
        if (!(n instanceof String) || !((String) n).startsWith("--"))
            return null;
        return (String) n;
    }

    public static void filter(final List<Object> l, final List<Object> o, final Predicate<Object> filter) {
        for (final Object obj : l)
            if (filter.test(obj))
                o.add(obj);
    }

    public static <T extends Enum<T>> void filterEnum(final List<Object> l, final List<Object> o, final Class<T> e) {
        filter(l, o, obj -> {
            if (!(obj instanceof String))
                return false;
            try {
                MiniUtil.enumValueOfIgnoreCase(e, ((String) obj).replace('-', '_'));
                return true;
            } catch (final IllegalAccessException ignored) {
                return false;
            }
        });
    }

    public static void filterNumbers(final List<Object> l, final List<Object> o) {
        m:
        for (final Object obj : l)
            if (obj instanceof StyleCall) {
                final StyleCall c = (StyleCall) obj;
                if ("calc".equalsIgnoreCase(c.name))
                    o.add(obj);
            } else if (obj instanceof String) {
                String n = ((String) obj).toLowerCase();
                if (n.endsWith("deg"))
                    n = n.substring(0, n.length() - 3);
                else if (n.endsWith("px") || n.endsWith("dp") || n.endsWith("sp"))
                    n = n.substring(0, n.length() - 2);
                else if (n.endsWith("%"))
                    n = n.substring(0, n.length() - 1);
                if (n.isEmpty())
                    continue;
                for (final char ch : n.toCharArray())
                    if (!Character.isDigit(ch))
                        continue m;
                o.add(obj);
            } else if (
                    obj instanceof Float ||
                            obj instanceof Integer
            )
                o.add(obj);
    }

    public static Object get(final List<Object> r, int i) {
        if (r.isEmpty())
            return null;
        if (Math.floorMod(r.size(), 4) == 3) {
            final int d = Math.floorDiv(i, 4);
            if (Math.floorMod(i, 4) == 3)
                i++;
            i -= d;
        }
        i = Math.floorMod(i, r.size());
        return r.get(i);
    }

    public static Object getNumber(final List<Object> l, int i) {
        final List<Object> r = tr.get();
        try {
            filterNumbers(l, r);
            return get(r, i);
        } finally {
            r.clear();
        }
    }

    @Override
    public String toString() {
        return name + " = " + objs;
    }
}