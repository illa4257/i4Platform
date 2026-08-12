package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Utils.MiniUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;

import static illa4257.i4Framework.base.Framework.L;

public class StyleProperty {
    public final String name;
    public final List<List<List<Object>>> objs = new ArrayList<>();

    public StyleProperty(final String name) {
        this.name = Objects.requireNonNull(name).toLowerCase();
    }

    public static final int
            TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3,
            TOP_LEFT = 0, TOP_RIGHT = 1, BOTTOM_RIGHT = 2, BOTTOM_LEFT = 3
    ;

    public static String hex2str(final String s) {
        final int c = Integer.parseInt(s, 16);
        return Character.isValidCodePoint(c) ? new String(Character.toChars(c)) : "\uFFFD";
    }

    public static StyleProperty parse(final String name, final String value) {
        final StyleProperty property = new StyleProperty(name);
        final StringBuilder text = new StringBuilder(), special = new StringBuilder(6);
        final Stack<List<List<List<Object>>>> stack = new Stack<>();
        final Stack<Boolean> calcs = new Stack<>();
        List<List<List<Object>>> layers = property.objs; // ,
        List<List<Object>> sets = new ArrayList<>(); // /
        List<Object> values = new ArrayList<>(); // ' '
        byte state = 0;
        boolean escape = false, isCalc = false;
        for (final char ch : value.toCharArray())
            switch (state) {
                case 0:
                    switch (ch) {
                        case '\t':
                        case ' ':
                            if (text.length() == 0)
                                break;
                            values.add(text.toString());
                            text.setLength(0);
                            break;
                        case '(':
                            final StyleCall call = new StyleCall(text.toString().toLowerCase());
                            text.setLength(0);
                            values.add(call);
                            sets.add(values);
                            layers.add(sets);

                            stack.push(layers);
                            calcs.push(isCalc);
                            isCalc = call.name.equals("calc");
                            layers = call.objs;
                            sets = new ArrayList<>();
                            values = new ArrayList<>();
                            break;
                        case ')':
                            if (text.length() > 0) {
                                values.add(text.toString());
                                text.setLength(0);
                            }
                            if (!values.isEmpty())
                                sets.add(values);
                            if (!sets.isEmpty())
                                layers.add(sets);
                            if (stack.isEmpty()) {
                                layers = property.objs;
                                isCalc = false;
                                sets = layers.remove(layers.size() - 1);
                                values = sets.remove(sets.size() - 1);
                                break;
                            }
                            layers = stack.pop();
                            isCalc = calcs.pop();
                            sets = layers.remove(layers.size() - 1);
                            values = sets.remove(sets.size() - 1);
                            break;
                        case ',':
                            if (text.length() > 0) {
                                values.add(text.toString());
                                text.setLength(0);
                            }
                            if (!values.isEmpty()) {
                                sets.add(values);
                                values = new ArrayList<>();
                            }
                            if (!sets.isEmpty()) {
                                layers.add(sets);
                                sets = new ArrayList<>();
                            }
                            break;
                        case '\\':
                            state = 1;
                            break;
                        case '\'':
                            if (text.length() > 0) {
                                values.add(text.toString());
                                text.setLength(0);
                            }
                            state = 2;
                            break;
                        case '"':
                            if (text.length() > 0) {
                                values.add(text.toString());
                                text.setLength(0);
                            }
                            state = 3;
                            break;
                        case '/':
                            if (text.length() > 0) {
                                values.add(text.toString());
                                text.setLength(0);
                            }
                            if (isCalc) {
                                values.add(ch);
                                break;
                            }
                            if (!values.isEmpty()) {
                                sets.add(values);
                                values = new ArrayList<>();
                            }
                            break;
                        case '+':
                        case '-':
                        case '*':
                            if (isCalc) {
                                if (text.length() > 0) {
                                    values.add(text.toString());
                                    text.setLength(0);
                                }
                                values.add(ch);
                                break;
                            }
                        default:
                            text.append(ch);
                            break;
                    }
                    break;
                case 1:
                    text.append(ch);
                    state = 0;
                    break;
                case 2:
                    switch (ch) {
                        case '\\':
                            if (!escape) {
                                escape = true;
                                break;
                            }
                            if (special.length() == 0) {
                                escape = false;
                                text.append('\\');
                                break;
                            }
                            text.append(hex2str(special.toString()));
                            special.setLength(0);
                            break;
                        case '\'':
                            if (escape) {
                                escape = false;
                                if (special.length() == 0) {
                                    text.append('\'');
                                    break;
                                }
                                text.append(hex2str(special.toString()));
                                special.setLength(0);
                            }
                            values.add(new StyleStr(text.toString()));
                            text.setLength(0);
                            state = 0;
                            break;
                        default:
                            if (escape) {
                                escape = false;
                                if (
                                        (ch >= '0' && ch <= '9') ||
                                        (ch >= 'a' && ch <= 'f') ||
                                        (ch >= 'A' && ch <= 'F')
                                ) {
                                    if (special.append(ch).length() != 6)
                                        break;
                                    text.append(hex2str(special.toString()));
                                    special.setLength(0);
                                    break;
                                }
                                if (special.length() > 0) {
                                    text.append(hex2str(special.toString()));
                                    special.setLength(0);
                                }
                            }
                            text.append(ch);
                            break;
                    }
                    break;
                case 3:
                    switch (ch) {
                        case '\\':
                            if (!escape) {
                                escape = true;
                                break;
                            }
                            if (special.length() == 0) {
                                escape = false;
                                text.append('\\');
                                break;
                            }
                            text.append(hex2str(special.toString()));
                            special.setLength(0);
                            break;
                        case '\"':
                            if (escape) {
                                escape = false;
                                if (special.length() == 0) {
                                    text.append('\"');
                                    break;
                                }
                                text.append(hex2str(special.toString()));
                                special.setLength(0);
                            }
                            values.add(new StyleStr(text.toString()));
                            text.setLength(0);
                            state = 0;
                            break;
                        default:
                            if (escape) {
                                escape = false;
                                if (
                                        (ch >= '0' && ch <= '9') ||
                                        (ch >= 'a' && ch <= 'f') ||
                                        (ch >= 'A' && ch <= 'F')
                                ) {
                                    if (special.append(ch).length() != 6)
                                        break;
                                    text.append(hex2str(special.toString()));
                                    special.setLength(0);
                                    break;
                                }
                                if (special.length() > 0) {
                                    text.append(hex2str(special.toString()));
                                    special.setLength(0);
                                }
                            }
                            text.append(ch);
                            break;
                    }
                    break;
            }
        if (special.length() > 0)
            text.append(hex2str(special.toString()));
        if (text.length() > 0) {
            if (state == 2 || state == 3)
                values.add(new StyleStr(text.toString()));
            else
                values.add(text.toString());
        }
        if (!values.isEmpty())
            sets.add(values);
        if (!sets.isEmpty())
            layers.add(sets);
        return property;
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
    }, pxFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "calc".equals(c.name);
        } else if (obj instanceof String) {
            String n = ((String) obj).toLowerCase();
            if (n.equals("auto"))
                return true;
            if (n.endsWith("px") || n.endsWith("dp") || n.endsWith("sp"))
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
    }, rotationFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "calc".equals(c.name);
        } else if (obj instanceof String) {
            String n = ((String) obj).toLowerCase();
            if (n.endsWith("deg"))
                n = n.substring(0, n.length() - 3);
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
        if (obj instanceof Paint)
            return true;
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
    }, spriteFilter = obj -> {
        if (obj instanceof StyleCall) {
            final StyleCall c = (StyleCall) obj;
            return "url".equals(c.name);
        }
        return false;
    };

    public static <T extends Enum<T>> Predicate<Object> enumFilter(final Class<T> e) {
        return o -> {
            if (e.isInstance(o))
                return true;
            if (o instanceof String)
                try {
                    MiniUtil.enumValueOfIgnoreCase(e, ((String) o).replace('-', '_'));
                    return true;
                } catch (final IllegalAccessException ignored) {
                    return false;
                }
            return false;
        };
    }

    public static String getVarName(final Object o) {
        if (!(o instanceof StyleCall))
            return null;
        final StyleCall c = (StyleCall) o;
        if (!"var".equals(c.name) || c.objs.isEmpty() || c.objs.get(0).isEmpty() || c.objs.get(0).get(0).isEmpty())
            return null;
        final Object n = c.objs.get(0).get(0).get(0);
        if (n instanceof String)
            return ((String) n).startsWith("--") ? (String) n : null;
        return null;
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

    public static Paint toPaint(final Object o, final Paint defValue) {
        if (o instanceof Paint)
            return (Paint) o;
        if (o instanceof String)
            try {
                return Color.parse((String) o);
            } catch (final IllegalArgumentException ex) {
                L.w(ex);
            }
        return defValue;
    }
}