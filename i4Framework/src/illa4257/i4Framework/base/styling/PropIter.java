package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.graphics.Sprite;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Utils.MiniUtil;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static illa4257.i4Framework.base.Framework.L;

public class PropIter implements Iterable<Object>, Iterator<Object> {
    public static final ThreadLocal<PropIter>
            INSTANCE = ThreadLocal.withInitial(PropIter::new),
            INSTANCE2 = ThreadLocal.withInitial(PropIter::new);

    public List<Object> values = Collections.emptyList();
    public final List<Object> filteredValues = new ArrayList<>();

    private final Stack<String> propStack = new Stack<>();
    private final Stack<Iterator<Object>> iterStack = new Stack<>();
    private Iterator<Object> iter = null;

    private int layer = 0, set = 0, index = 0;
    private Object next = null;

    private Component component = null;
    private Function<List<String>, StyleProperty> lookupPropertyL;
    private Function<String[], StyleProperty> lookupPropertySA;
    private Function<String, StyleProperty> lookupPropertyS;

    private Predicate<Object> filter = ignored -> true;

    private StyleProperty property = null;

    public PropIter setComponent(final Component component) {
        if (this.component == component)
            return this;
        this.component = component;
        this.lookupPropertyL = component::getProperty;
        this.lookupPropertySA = component::getProperty;
        this.lookupPropertyS = component::getProperty;
        filter = ignored -> true;
        layer = -1;
        set = 0;
        index = 0;
        propStack.clear();
        iterStack.clear();
        iter = null;
        values = Collections.emptyList();
        filteredValues.clear();
        return this;
    }

    public PropIter select(final StyleProperty property, final Predicate<Object> filter) {
        this.property = property;
        this.filter = filter;
        layer = -1;
        set = 0;
        index = 0;
        propStack.clear();
        iterStack.clear();
        iter = null;
        values = Collections.emptyList();
        filteredValues.clear();
        return this;
    }

    public PropIter select(final List<String> propertyNames, final Predicate<Object> filter) {
        return select(lookupPropertyL.apply(propertyNames), filter);
    }

    public PropIter select(final String[] propertyNames, final Predicate<Object> filter) {
        return select(lookupPropertySA.apply(propertyNames), filter);
    }

    public PropIter select(final String propertyName, final Predicate<Object> filter) {
        return select(lookupPropertyS.apply(propertyName), filter);
    }

    public <T extends Enum<T>> PropIter select(final StyleProperty property, final Class<T> e) {
        return select(property, StyleProperty.enumFilter(e));
    }

    public <T extends Enum<T>> PropIter select(final List<String> propertyNames, final Class<T> e) {
        return select(lookupPropertyL.apply(propertyNames), StyleProperty.enumFilter(e));
    }

    public <T extends Enum<T>> PropIter select(final String[] propertyNames, final Class<T> e) {
        return select(lookupPropertySA.apply(propertyNames), StyleProperty.enumFilter(e));
    }

    public <T extends Enum<T>> PropIter select(final String propertyName, final Class<T> e) {
        return select(lookupPropertyS.apply(propertyName), StyleProperty.enumFilter(e));
    }

    public boolean hasNextLayer() {
        return property != null && property.objs.size() > layer + 1;
    }

    public PropIter nextLayer() {
        if (property == null || property.objs.size() <= layer + 1)
            return this;
        layer++;
        set = 0;
        index = 0;
        propStack.clear();
        iterStack.clear();
        iter = null;
        values = Collections.emptyList();
        filteredValues.clear();
        next = null;
        return this;
    }

    public boolean hasNextSet() {
        return property != null && property.objs.size() > layer && property.objs.get(layer).size() > set;
    }

    public PropIter nextSet() {
        if (property == null || property.objs.size() <= layer)
            return this;
        final List<List<Object>> r = property.objs.get(layer);
        if (r.size() <= set)
            return this;
        values = r.get(set++);
        propStack.clear();
        iterStack.clear();
        if (property.name != null)
            propStack.push(property.name.toLowerCase());
        iter = values.iterator();
        filteredValues.clear();
        index = 0;
        next = null;
        return this;
    }

    public boolean hasNext() {
        if (values.isEmpty())
            return false;
        if (next != null)
            return true;
        while (iter != null) {
            while (iter.hasNext()) {
                final Object o = iter.next();
                if (o instanceof StyleCall && "var".equals(((StyleCall) o).name)) {
                    List<List<List<Object>>> l = ((StyleCall) o).objs;
                    if (l.isEmpty())
                        continue;
                    List<List<Object>> l2 = l.get(0);
                    if (l2.isEmpty())
                        continue;
                    List<Object> l3 = l2.get(0);
                    if (l3.isEmpty())
                        continue;
                    final Object n = l3.get(0);
                    if (!(n instanceof String))
                        continue;
                    final StyleProperty p = lookupPropertyS.apply((String) n);
                    if (p == null)
                        continue;
                    l = p.objs;
                    if (l.isEmpty())
                        continue;
                    l2 = l.get(0);
                    if (l2.isEmpty())
                        continue;
                    l3 = l2.get(0);
                    if (l3.isEmpty())
                        continue;
                    final String name = p.name.toLowerCase();
                    if (propStack.contains(name))
                        continue;
                    propStack.push(name);
                    iterStack.push(iter);
                    iter = l3.iterator();
                    continue;
                }
                if (filter.test(o)) {
                    index++;
                    filteredValues.add(o);
                    next = o;
                    return true;
                }
            }
            if (iterStack.isEmpty())
                iter = null;
            else {
                propStack.pop();
                iter = iterStack.pop();
            }
        }
        return index < filteredValues.size();
    }

    public Object next(final Supplier<?> s) {
        if (values.isEmpty())
            return s != null ? s.get() : null;
        if (next != null) {
            final Object r = next;
            next = null;
            return r;
        }
        while (iter != null) {
            while (iter.hasNext()) {
                final Object o = iter.next();
                if (o instanceof StyleCall && "var".equals(((StyleCall) o).name)) {
                    List<List<List<Object>>> l = ((StyleCall) o).objs;
                    if (l.isEmpty())
                        continue;
                    List<List<Object>> l2 = l.get(0);
                    if (l2.isEmpty())
                        continue;
                    List<Object> l3 = l2.get(0);
                    if (l3.isEmpty())
                        continue;
                    final Object n = l3.get(0);
                    if (!(n instanceof String))
                        continue;
                    final StyleProperty p = lookupPropertyS.apply((String) n);
                    if (p == null)
                        continue;
                    l = p.objs;
                    if (l.isEmpty())
                        continue;
                    l2 = l.get(0);
                    if (l2.isEmpty())
                        continue;
                    l3 = l2.get(0);
                    if (l3.isEmpty())
                        continue;
                    final String name = p.name.toLowerCase();
                    if (propStack.contains(name))
                        continue;
                    propStack.push(name);
                    iterStack.push(iter);
                    iter = l3.iterator();
                    continue;
                }
                if (filter.test(o)) {
                    index++;
                    filteredValues.add(o);
                    return o;
                }
            }
            if (iterStack.isEmpty())
                iter = null;
            else {
                propStack.pop();
                iter = iterStack.pop();
            }
        }
        if (filteredValues.isEmpty())
            return s != null ? s.get() : null;
        int i = index++;
        if (Math.floorMod(filteredValues.size(), 4) == 3) {
            final int d = Math.floorDiv(i, 4);
            if (Math.floorMod(i, 4) == 3)
                i++;
            i -= d;
        }
        return filteredValues.get(Math.floorMod(i, filteredValues.size()));
    }

    public Object next(final Object defValue) {
        return next(() -> defValue);
    }

    @Override
    public Object next() { return next(() -> null); }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<Object> iterator() {
        return this;
    }

    public Paint paint(final Paint defValue) {
        return StyleProperty.toPaint(next(), defValue);
    }

    public Point point(final Object o, final Point parent, final Point defValue) {
        if (o instanceof Point)
            return (Point) o;
        if (o instanceof Float)
            return new NumberPointConstant((float) o);
        if (o instanceof String) {
            final String s = ((String) o).toLowerCase();
            if (s.equals("auto"))
                return null;
            if (s.endsWith("deg"))
                return new NumberPointConstant(Float.parseFloat(s.substring(0, s.length() - 3)));
            if (s.endsWith("%")) {
                final float v = Float.parseFloat(s.substring(0, s.length() - 1));
                return parent != null ? new NumberPointMultiplier(v, parent) : new NumberPointConstant(v);
            }
            if (s.endsWith("px"))
                return new NumberPointConstant(Float.parseFloat(s.substring(0, s.length() - 2)));
            if (s.endsWith("dp"))
                if (component != null)
                    return new NumberPointMultiplier(Float.parseFloat(s.substring(0, s.length() - 2)), component.dp);
                else
                    return new NumberPointConstant(Float.parseFloat(s.substring(0, s.length() - 2)));
            if (s.endsWith("sp"))
                if (component != null)
                    return new NumberPointMultiplier(Float.parseFloat(s.substring(0, s.length() - 2)), component.sp);
                else
                    return new NumberPointConstant(Float.parseFloat(s.substring(0, s.length() - 2)));
            return new NumberPointConstant(Float.parseFloat(s));
        } else if (o != null)
            L.w("Unknown number type", o.getClass());
        return defValue;
    }

    public Point point(final Point parent, final Point defValue) {
        return point(next, parent, defValue);
    }

    public Point point(final Orientation orientation, final Point defValue) {
        final Container c = component != null ? component.getParent() : null;
        return point(
                next(),
                orientation != null && c != null ? orientation == Orientation.HORIZONTAL ? c.width : c.height : null,
                defValue
        );
    }

    public float f(final Object o, final Point parent, final float defValue) {
        if (o instanceof Point)
            return ((Point) o).calcFloat();
        if (o instanceof Float)
            return (float) o;
        if (o instanceof String) {
            final String s = (String) o;
            if (s.endsWith("deg"))
                return Float.parseFloat(s.substring(0, s.length() - 3));
            if (s.endsWith("%")) {
                final float v = Float.parseFloat(s.substring(0, s.length() - 1));
                return parent != null ? v * parent.calcFloat() : v;
            }
            if (s.endsWith("px"))
                return Float.parseFloat(s.substring(0, s.length() - 2));
            if (s.endsWith("dp"))
                if (component != null)
                    return Float.parseFloat(s.substring(0, s.length() - 2)) * component.dp.calcFloat();
                else
                    return Float.parseFloat(s.substring(0, s.length() - 2));
            if (s.endsWith("sp"))
                if (component != null)
                    return Float.parseFloat(s.substring(0, s.length() - 2)) * component.sp.calcFloat();
                else
                    return Float.parseFloat(s.substring(0, s.length() - 2));
            return Float.parseFloat(s);
        } else if (o != null)
            L.w("Unknown number type", o.getClass());
        return defValue;
    }

    public float f(final Object o, final float parent, final float defValue) {
        if (o instanceof Point)
            return ((Point) o).calcFloat();
        if (o instanceof Float)
            return (float) o;
        if (o instanceof String) {
            final String s = (String) o;
            if (s.endsWith("deg"))
                return Float.parseFloat(s.substring(0, s.length() - 3));
            if (s.endsWith("%")) {
                final float v = Float.parseFloat(s.substring(0, s.length() - 1));
                return v * parent;
            }
            if (s.endsWith("px"))
                return Float.parseFloat(s.substring(0, s.length() - 2));
            if (s.endsWith("dp"))
                if (component != null)
                    return Float.parseFloat(s.substring(0, s.length() - 2)) * component.dp.calcFloat();
                else
                    return Float.parseFloat(s.substring(0, s.length() - 2));
            if (s.endsWith("sp"))
                if (component != null)
                    return Float.parseFloat(s.substring(0, s.length() - 2)) * component.sp.calcFloat();
                else
                    return Float.parseFloat(s.substring(0, s.length() - 2));
            return Float.parseFloat(s);
        } else if (o != null)
            L.w("Unknown number type", o.getClass());
        return defValue;
    }

    public float f(final Point parent, final float defValue) {
        return f(next(), parent, defValue);
    }

    public float f(final float parent, final float defValue) {
        return f(next(), parent, defValue);
    }

    public float f(final Orientation orientation, final float defValue) {
        final Container c = component != null ? component.getParent() : null;
        return f(
                next(),
                orientation != null && c != null ? orientation == Orientation.HORIZONTAL ? c.width : c.height : null,
                defValue
        );
    }

    public Sprite sprite(final Object o, final Sprite defValue) {
        if (o instanceof Sprite)
            return (Sprite) o;
        if (o instanceof StyleCall) {
            final StyleCall c = (StyleCall) o;
            if ("url".equals(c.name) && !c.objs.isEmpty() && !c.objs.get(0).isEmpty()) {
                final List<Object> l = c.objs.get(0).get(0);
                if (!l.isEmpty()) {
                    final Object url = l.get(0);
                    if (url instanceof StyleStr) {
                        final String s = ((StyleStr) url).value;
                        final AtomicReference<Sprite> ir = new AtomicReference<>();
                        {
                            final SoftReference<Sprite> ref = Cache.sprites.computeIfAbsent(s, ignored -> {
                                final Framework f = component.getFramework();
                                if (f != null)
                                    try {
                                        final Sprite img = f.getSprite(s);
                                        ir.set(img);
                                        return new SoftReference<>(img);
                                    } catch (final Exception ex) {
                                        L.e(ex);
                                    }
                                return null;
                            });
                            if (ir.get() != null)
                                return ir.get();
                            if (ref != null) {
                                final Sprite r = ref.get();
                                if (r != null)
                                    return r;
                            }
                        }
                        Cache.sprites.compute(s, (ignored, ref) -> {
                            if (ref != null) {
                                final Sprite img = ref.get();
                                if (img != null) {
                                    ir.set(img);
                                    return ref;
                                }
                            }
                            final Framework f = component.getFramework();
                            if (f != null)
                                try {
                                    final Sprite img = f.getSprite(s);
                                    ir.set(img);
                                    return new SoftReference<>(img);
                                } catch (final Exception ex) {
                                    L.e(ex);
                                }
                            return null;
                        });
                        if (ir.get() != null)
                            return ir.get();
                    }
                }
            }
        }
        return defValue;
    }

    public Sprite sprite(final Sprite defValue) {
        return sprite(next(), defValue);
    }

    public <T extends Enum<T>> T e(final Object o, final Class<T> enumClass, final T defValue) {
        if (enumClass.isInstance(o))
            return enumClass.cast(o);
        if (o instanceof String)
            try {
                return MiniUtil.enumValueOfIgnoreCase(enumClass, ((String) o).replace('-', '_'));
            } catch (final IllegalAccessException ignored) {
                return defValue;
            }
        return defValue;
    }

    public <T extends Enum<T>> T e(final Class<T> enumClass, final T defValue) {
        return e(next(), enumClass, defValue);
    }
}