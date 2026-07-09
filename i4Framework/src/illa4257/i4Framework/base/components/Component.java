package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.*;
import illa4257.i4Framework.base.curves.Curve;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.IEvent;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.dnd.DropEvent;
import illa4257.i4Framework.base.events.keyboard.KeyEvent;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseEnterEvent;
import illa4257.i4Framework.base.events.mouse.MouseLeaveEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchDownEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.points.numbers.NumberPointAdd;
import illa4257.i4Framework.base.points.ops.PPointAdd;
import illa4257.i4Framework.base.points.ops.PPointSubtract;
import illa4257.i4Framework.base.styling.*;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.points.*;
import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Utils.Destructor;
import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.lists.IntSet;
import illa4257.i4Utils.lists.SwappableTmpQueue;
import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static illa4257.i4Framework.base.Framework.L;

@SuppressWarnings("UnusedReturnValue")
public class Component extends Destructor {
    protected final Object locker = new Object();
    volatile boolean isFocusable = false, visible = true;

    public volatile Object redirectFocus = null;

    protected final SyncVar<Container> parent = new SyncVar<>();

    protected final ConcurrentLinkedQueue<Runnable> repeatedInvoke = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean isRepeated = new AtomicBoolean(false);
    private final SwappableTmpQueue<Runnable> invoke = new SwappableTmpQueue<>();

    private final ConcurrentHashMap<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<? extends IEvent>>>
        eventListeners = new ConcurrentHashMap<>(),
        directEventListeners = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<IEvent> events = new ConcurrentLinkedQueue<>();

    public final AtomicReference<String> id = new AtomicReference<>(), tag = new AtomicReference<>();
    public final ConcurrentLinkedQueue<String> classes = new ConcurrentLinkedQueue<>(), pseudoClasses = new ConcurrentLinkedQueue<>();

    private final AtomicInteger lsx = new AtomicInteger(), lsy = new  AtomicInteger(),
            lex = new AtomicInteger(), ley = new  AtomicInteger();

    public final Style style = new Style();
    public final Stylesheet stylesheet = new Stylesheet();
    private final ArrayList<Map.Entry<StyleSelector, Style>> cache = new ArrayList<>();
    private final ConcurrentHashMap<String, List<List<Object>>> cachedEvals = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>>>
            subscribers = new ConcurrentHashMap<>();

    private static class ArrKeys {
        public final String[] array;
        public final int hashCode;

        public ArrKeys(final String[] array) { this.array = array; this.hashCode = Arrays.hashCode(array); }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ArrKeys)
                return ((ArrKeys) obj).hashCode == hashCode;
            return super.equals(obj);
        }
    }

    private volatile Point propL = null, propT = null, propR = null, propB = null, propW = null, propH = null;
    private final PointSet sx = new PointSet(), sy = new PointSet(), ex = new PointSet(), ey = new PointSet();
    public final Point styleSX = sx, styleSY = sy, styleEX = ex, styleEY = ey;

    public final PointSet
            startX = new PointSet(styleSX),
            startY = new PointSet(styleSY),
            endX = new PointSet(styleEX),
            endY = new PointSet(styleEY),
            dp = new PointSet(NumberPointConstant.ONE), sp = new PointSet(dp);

    public final Point
            width = new PPointSubtract(endX, startX), height = new PPointSubtract(endY, startY),

            offsetSX = getPoint(outlineWidthProperties, Orientation.HORIZONTAL, 0, 0, 0),
            offsetSY = offsetSX,
            offsetEX = offsetSX,
            offsetEY = offsetSX,
            renderStartX = new PPointSubtract(startX, offsetSX), renderStartY = new PPointSubtract(startY, offsetSY),
            renderEndX = new PPointAdd(endX, offsetEX), renderEndY = new PPointAdd(endY, offsetEY),
            renderWidth = new PPointSubtract(renderEndX, renderStartX),
            renderHeight = new PPointSubtract(renderEndY, renderStartY),

            windowStartX = new PPointAdd(startX, null), windowStartY = new PPointAdd(startY, null),
            windowEndX = new PPointAdd(endX, null), windowEndY = new PPointAdd(endY, null);

    public Component() {
        Class<?> c = getClass();
        while ((c.isAnonymousClass() || c.isLocalClass()) && c.getSuperclass() != null)
            c = c.getSuperclass();
        tag.set(c.getSimpleName());
        pseudoClasses.add("enabled");
        addEventListener(ReCalcCheckEvent.class, e -> {
            final int sx = Float.floatToIntBits(renderStartX.calcFloat()), sy = Float.floatToIntBits(renderStartY.calcFloat()),
                    ex = Float.floatToIntBits(renderEndX.calcFloat()), ey = Float.floatToIntBits(renderEndY.calcFloat());
            if (lsx.getAndSet(sx) != sx || lsy.getAndSet(sy) != sy || lex.getAndSet(ex) != ex || ley.getAndSet(ey) != ey)
                fire(new RecalculateEvent(Component.this));
        });
        addEventListener(ChangeParentEvent.class, e -> {
            final Container co = getParent();
            if (co != null) {
                ((PPointAdd) windowStartX).setPoint2(co.windowStartX);
                ((PPointAdd) windowStartY).setPoint2(co.windowStartY);
                ((PPointAdd) windowEndX).setPoint2(co.windowStartX);
                ((PPointAdd) windowEndY).setPoint2(co.windowStartY);
            }
            fire(new StyleUpdateEvent(this));
        });
        addEventListener(StyleUpdateEvent.class, e -> {
            synchronized (cache) {
                for (final Map.Entry<StyleSelector, Style> entry : cache)
                    entry.getValue().unsubscribe(this::onPropertyChange);
                final String[] ok = cachedEvals.keySet().toArray(new String[0]);
                Arrays.sort(ok);
                final HashMap<ArrKeys, StyleProperty> m = new HashMap<>();
                for (final ConcurrentHashMap<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>> entry : subscribers.values())
                    for (final ArrKeys ak : entry.keySet())
                        m.computeIfAbsent(ak, ignored -> {
                            final StyleProperty p = getProperty(ak.array);
                            return p != null && Arrays.binarySearch(ok, p.name) >= 0 ? p : null;
                        });
                cache.clear();
                cachedEvals.clear();
                if (!isConstructed())
                    return;
                cache.add(new AbstractMap.SimpleImmutableEntry<>(null, style));
                final ArrayList<StyleSelector> selectors = new ArrayList<>();
                cacheStyles(this, selectors);
                final Framework framework = getFramework();
                if (framework != null)
                    cacheStyles(framework.stylesheet, selectors);
                for (final Map.Entry<StyleSelector, Style> entry : cache)
                    entry.getValue().subscribe(this::onPropertyChange);
                final IntSet set = new IntSet();
                for (final ConcurrentHashMap<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>> le : subscribers.values())
                    for (final Map.Entry<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>> entry : le.entrySet())
                        if (set.add(entry.getKey().hashCode)) {
                            final StyleProperty p = getProperty(entry.getKey().array);
                            if (m.get(entry.getKey()) == p)
                                continue;
                            for (final Consumer<StyleProperty> l : entry.getValue())
                                l.accept(p);
                        }
            }
        });
        addEventListener(HoverEvent.class, e -> {
            if (e.component == this)
                setPseudoClass("hover", e.value);
        });
        addEventListener(FocusEvent.class, e -> {
            if (e.component == this)
                setPseudoClass("focus", e.value);
        });
        addEventListener(MouseEnterEvent.class, e -> {
            if (e.component == this) {
                setPseudoClass("hover", true);
                fire(new HoverEvent(this, true));
            }
            setPseudoClass("hover-within", true);
        });
        addEventListener(MouseLeaveEvent.class, e -> {
            if (e.component == this) {
                setPseudoClass("hover", false);
                fire(new HoverEvent(this, false));
            }
            setPseudoClass("hover-within", pseudoClasses.contains("hover"));
        });
    }

    public boolean isVisible() { return visible; }
    public boolean isEnabled() { return pseudoClasses.contains("enabled"); }
    public boolean isFocusable() { return isFocusable; }
    public boolean isFocused() { return pseudoClasses.contains("focus"); }
    public boolean isFocusedWithin() { return pseudoClasses.contains("focus-within"); }
    public boolean isRepeated() { return isRepeated.get(); }
    public Component find(final float x, final float y, final float[] localPos) {
        if (startX.calcFloat() < x && endX.calcFloat() > x &&
                startY.calcFloat() < y && endY.calcFloat() > y) {
            localPos[0] = x - startX.calcFloat();
            localPos[1] = y - startY.calcFloat();
            return this;
        }
        return null;
    }

    protected void cacheStyles(
            final Stylesheet stylesheet,
            final ArrayList<StyleSelector> selectors) {
        int l = selectors.size();
        for (final Map.Entry<StyleSelector, Style> e : stylesheet.stylesheet)
            if (e.getKey().check(this)) {
                int i = 0;
                for (; i < l; i++) {
                    final boolean co = compareSelectors(e.getKey(), selectors.get(i));
                    if (co)
                        continue;
                    break;
                }
                cache.add(i + 1, e);
                selectors.add(i, e.getKey());
                l++;
            }
    }

    protected void cacheStyles(final Component c, final ArrayList<StyleSelector> selectors) {
        cacheStyles(c.stylesheet, selectors);
        final Container p = c.getParent();
        if (p != null)
            cacheStyles(p, selectors);
    }

    private boolean compareSelectors(final StyleSelector selector1, final StyleSelector selector2) {
        if (!selector1.isIdEmpty()) {
            if (selector2.isIdEmpty())
                return false;
        } else if (!selector2.isIdEmpty())
            return true;

        final int c1 = selector1.classes.size(), c2 = selector2.classes.size();

        if (c1 > c2 || c2 > c1)
            return c2 > c1;

        final int pc1 = selector1.pseudoClasses.size(), pc2 = selector2.pseudoClasses.size();
        if (pc1 != 0 || pc2 != 0)
            return pc2 >= pc1;

        /* Extended
        if (selector1.tag.get() != null && selector2.tag.get() == null)
            return false;
        return true;*/

        return selector1.tag.get() == null || selector2.tag.get() != null;
    }

    public void setPseudoClass(final String pseudoClass, final boolean en) {
        if (pseudoClass == null || pseudoClass.isEmpty() || pseudoClasses.contains(pseudoClass) == en)
            return;
        final HashMap<ArrKeys, StyleProperty> m = new HashMap<>();
        for (final ConcurrentHashMap<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>> e : subscribers.values())
            for (final ArrKeys ak : e.keySet())
                m.computeIfAbsent(ak, ignored -> getProperty(ak.array));
        if (en)
            pseudoClasses.offer(pseudoClass);
        else
            pseudoClasses.remove(pseudoClass);
        cachedEvals.clear();
        for (final Map.Entry<ArrKeys, StyleProperty> e : m.entrySet()) {
            final StyleProperty n = getProperty(e.getKey().array);
            if (n == e.getValue())
                continue;
            onPropertyChange(n);
        }
        repaint();
    }

    private void onPropertyChange(final StyleProperty property) {
        L.d("onPropertyChange", property);
    }

    /// @param properties It should be immutable and sorted via {@link Arrays#sort(Object[])}.
    public void subscribe(final String[] properties, final Consumer<StyleProperty> listener) {
        final ArrKeys ak = new ArrKeys(properties);
        for (final String p : properties)
            subscribers.computeIfAbsent(p, ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(ak, ignored -> new ConcurrentLinkedQueue<>())
                    .offer(listener);
    }

    public void subscribe(final List<String> properties, final Consumer<StyleProperty> listener) {
        final String[] arr = properties.toArray(new String[0]);
        Arrays.sort(arr);
        subscribe(arr, listener);
    }

    public void subscribe(final String property, final Consumer<StyleProperty> listener) {
        subscribe(new String[] { property }, listener);
    }

    public void unsubscribe(final Consumer<StyleProperty> listener) {
        for (final ConcurrentHashMap<ArrKeys, ConcurrentLinkedQueue<Consumer<StyleProperty>>> e1 : subscribers.values())
            for (final ConcurrentLinkedQueue<Consumer<StyleProperty>> e2 : e1.values())
                e2.remove(listener);
    }

    public StyleProperty getProperty(final String name) {
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, Style> e : cache) {
                if (e.getKey() != null && !e.getKey().pseudoClasses.stream().allMatch(s -> pseudoClasses.stream()
                        .anyMatch(s::equalsIgnoreCase)))
                    continue;
                final StyleProperty s = e.getValue().get(name);
                if (s != null)
                    return s;
            }
            return null;
        }
    }

    public StyleProperty getProperty(final List<String> names) {
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, Style> e : cache) {
                if (e.getKey() != null && !e.getKey().pseudoClasses.stream().allMatch(s -> pseudoClasses.stream()
                        .anyMatch(s::equalsIgnoreCase)))
                    continue;
                final StyleProperty s = e.getValue().get(names);
                if (s != null)
                    return s;
            }
            return null;
        }
    }

    public StyleProperty getProperty(final String[] names) {
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, Style> e : cache) {
                if (e.getKey() != null && !e.getKey().pseudoClasses.stream().allMatch(s -> pseudoClasses.stream()
                        .anyMatch(s::equalsIgnoreCase)))
                    continue;
                final StyleProperty s = e.getValue().get(names);
                if (s != null)
                    return s;
            }
            return null;
        }
    }

    public List<List<Object>> resolveVar(final String varName, final ArrayList<String> inProcess) {
        if (varName == null)
            return Collections.emptyList();
        return cachedEvals.computeIfAbsent(varName, ignored -> {
            final StyleProperty property = getProperty(varName);
            if (property == null)
                return Collections.emptyList();
            if (inProcess.contains(varName))
                return Collections.emptyList();
            inProcess.add(varName);
            final List<List<Object>> ll = new ArrayList<>();
            for (final List<Object> l : property.objs) {
                final ArrayList<Object> n = new ArrayList<>();
                for (final Object o : l)
                    if (StyleProperty.varFilter.test(o))
                        for (final List<Object> nl : resolveVar(StyleProperty.getVarName(o), inProcess))
                            n.addAll(nl);
                    else
                        n.add(o);
                ll.add(n);
            }
            return ll;
        });
    }

    public List<List<Object>> evalVar(final StyleProperty property) {
        if (property == null)
            return null;
        return cachedEvals.computeIfAbsent(property.name, ignored -> {
            final ArrayList<String> inProcess = new ArrayList<>();
            inProcess.add(property.name);
            final List<List<Object>> ll = new ArrayList<>();
            for (final List<Object> l : property.objs) {
                final ArrayList<Object> n = new ArrayList<>();
                for (final Object o : l)
                    if (StyleProperty.varFilter.test(o))
                        for (final List<Object> nl : resolveVar(StyleProperty.getVarName(o), inProcess))
                            n.addAll(nl);
                    else
                        n.add(o);
                ll.add(n);
            }
            return ll;
        });
    }

    public List<List<Object>> evalVar(final String name) {
        return evalVar(getProperty(name));
    }

    public List<List<Object>> evalVar(final List<String> names) {
        return evalVar(getProperty(names));
    }

    private final ArrayList<EventListener<? extends IEvent>> focusListeners = new ArrayList<>();

    public void setFocusable(final boolean newValue) {
        synchronized (locker) {
            if (isFocusable == newValue)
                return;
            if (newValue)
                focusListeners.add(addEventListener(MouseDownEvent.class, e -> {
                    if (e.component == this)
                        requestFocus();
                }));
            else {
                removeEventListeners(focusListeners);
                focusListeners.clear();
            }
            isFocusable = newValue;
        }
    }

    public boolean requestFocus() {
        if (isFocused())
            return true;
        if (!isVisible())
            return false;
        final Container p = getParent();
        if (p == null)
            return false;
        return p.childFocus(this, this);
    }

    private final Runnable recalcCheck = () -> fire(new ReCalcCheckEvent(Component.this));

    @Override
    public void onConstruct() {
        renderWidth.subscribe(recalcCheck);
        renderHeight.subscribe(recalcCheck);
        subscribe("left", this::layoutLeft);
        subscribe("right", this::layoutRight);
        subscribe("width", this::layoutWidth);
        subscribe("top", this::layoutTop);
        subscribe("bottom", this::layoutBottom);
        subscribe("height", this::layoutHeight);
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, Style> entry : cache)
                entry.getValue().subscribe(this::onPropertyChange);
        }
    }

    @Override
    public void onDestruct() {
        renderWidth.unsubscribe(recalcCheck);
        renderHeight.unsubscribe(recalcCheck);
        unsubscribe(this::layoutLeft);
        unsubscribe(this::layoutRight);
        unsubscribe(this::layoutWidth);
        unsubscribe(this::layoutTop);
        unsubscribe(this::layoutBottom);
        unsubscribe(this::layoutHeight);
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, Style> entry : cache)
                entry.getValue().unsubscribe(this::onPropertyChange);
        }
    }

    private Point glp(final StyleProperty property, final Orientation orientation) {
        final List<Object> ss = Component.ss.get();

        ss.clear();
        getLayoutSet(evalVar(property), ss, 0);

        final float r = calc(ss, 0, orientation, Float.NaN);
        if (Float.isNaN(r))
            return null;
        return new NumberPointConstant(r);
    }

    private void layoutLeft(final StyleProperty property) {
        propL = glp(property, Orientation.HORIZONTAL);
        layoutH();
    }

    private void layoutTop(final StyleProperty property) {
        propT = glp(property, Orientation.VERTICAL);
        layoutV();
    }

    private void layoutRight(final StyleProperty property) {
        final Point r = glp(property, Orientation.HORIZONTAL);
        propR = r != null ? new PPointSubtract(new ParentPoint(this, Orientation.HORIZONTAL), r) : null;
        layoutH();
    }

    private void layoutBottom(final StyleProperty property) {
        final Point b = glp(property, Orientation.VERTICAL);
        propB = b != null ? new PPointSubtract(new ParentPoint(this, Orientation.VERTICAL), b) : null;
        layoutV();
    }

    private void layoutWidth(final StyleProperty property) {
        propW = glp(property, Orientation.HORIZONTAL);
        layoutH();
    }

    private void layoutHeight(final StyleProperty property) {
        propH = glp(property, Orientation.VERTICAL);
        layoutV();
    }

    private void layoutH() {
        final Point w = propW;
        if (w == null) {
            sx.set(propL);
            ex.set(propR);
        } else {
            final Point l = propL, r = propR;
            if (l == null) {
                ex.set(r);
                sx.set(new PPointSubtract(endX, w));
            } else {
                sx.set(l);
                ex.set(new PPointAdd(startX, w));
            }
        }
    }

    private void layoutV() {
        final Point h = propH;
        if (h == null) {
            sy.set(propT);
            ey.set(propB);
        } else {
            final Point t = propT, b = propB;
            if (t == null) {
                ey.set(b);
                sy.set(new PPointSubtract(endY, h));
            } else {
                sy.set(t);
                ey.set(new PPointAdd(startY, h));
            }
        }
    }

    public Object getLocker() { return locker; }
    public Container getParent() { return parent.get(); }
    public boolean remove() {
        final Container c = parent.get();
        return c != null && c.remove(this);
    }

    public Window getWindow() {
        final Container c = parent.get();
        return c != null ? c.getWindow() : null;
    }

    public Framework getFramework() {
        final Window w = getWindow();
        if (w == null)
            return null;
        return w.getFramework();
    }

    protected void updated() {
        final Framework f = getFramework();
        if (f != null)
            f.updated();
    }

    protected void invokeAll() {
        IEvent event;
        while ((event = events.poll()) != null)
            invoke(event);

        for (final Runnable r : invoke)
            try {
                r.run();
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
        for (final Runnable r : repeatedInvoke)
            try {
                r.run();
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    public void invoke(final IEvent event) {
        final Class<? extends IEvent> c = event.getClass();
        for (final Map.Entry<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<?>>> e : eventListeners.entrySet())
            if (c.isAssignableFrom(e.getKey()))
                for (@SuppressWarnings("rawtypes") final EventListener l : e.getValue()) {
                    try {
                        //noinspection unchecked
                        l.run(event);
                    } catch (final Throwable ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                    if (event.isPrevented())
                        return;
                }
        if (!event.isParentPrevented()) {
            final Container p = getParent();
            if (p == null)
                return;
            p.invoke(event);
        }
    }

    public void invokeLater(final Runnable runnable) { invoke.add(runnable); updated(); }

    public void invokeAndWait(final Runnable runnable) throws InterruptedException {
        if (Framework.isThread(this)) {
            runnable.run();
            return;
        }
        final Object l = new Object();
        synchronized (l) {
            invoke.add(() -> {
                try {
                    runnable.run();
                } catch (final Throwable throwable) {
                    i4Logger.INSTANCE.log(throwable);
                }
                synchronized (l) {
                    l.notifyAll();
                }
            });
            updated();
            l.wait();
        }
    }

    protected void repeated(final boolean v) {
        final Container c = getParent();
        if (c == null)
            return;
        c.repeated(v);
    }

    public void onTick(final Runnable runnable) {
        if (repeatedInvoke.add(runnable)) {
            isRepeated.set(true);
            repeated(true);
            updated();
        }
    }
    public void offTick(final Runnable runnable) {
        if (repeatedInvoke.remove(runnable) && repeatedInvoke.isEmpty()) {
            isRepeated.set(false);
            repeated(false);
        }
    }

    private <T extends IEvent> EventListener<T> addEventListenerInternal(final Class<T> eventType, final EventListener<T> listener, final ConcurrentHashMap<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<?>>> listeners) {
        if (listeners.computeIfAbsent(eventType, t -> new ConcurrentLinkedQueue<>()).add(listener))
            return listener;
        return null;
    }

    public <T extends IEvent> EventListener<T> addEventListener(final Class<T> eventType, final EventListener<T> listener) {
        return addEventListenerInternal(eventType, listener, eventListeners);
    }

    public <T extends IEvent> EventListener<T> addDirectEventListener(final Class<T> eventType, final EventListener<T> listener) {
        return addEventListenerInternal(eventType, listener, directEventListeners);
    }

    public <T extends IEvent> void removeEventListener(final EventListener<T> listener) {
        eventListeners.forEach((k, v) -> v.remove(listener));
    }

    public void removeEventListeners(final Collection<EventListener<? extends IEvent>> listeners) {
        eventListeners.forEach((k, v) -> v.removeAll(listeners));
    }

    public <T extends IEvent> boolean removeDirectEventListener(final EventListener<T> listener) {
        for (final Map.Entry<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<?>>> e : directEventListeners.entrySet())
            if (e.getValue().remove(listener))
                return true;
        return false;
    }

    public Object getRedirectFocus() {
        final Object o = redirectFocus;
        if (o != null)
            return o;
        final Container p = getParent();
        return p != null ? p.getRedirectFocus() : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void fire(final IEvent event) {
        if (event == null)
            return;
        if (event.isSystem() && (event instanceof MouseDownEvent || event instanceof MouseUpEvent ||
                event instanceof TouchDownEvent || event instanceof TouchUpEvent ||
                (event instanceof FocusEvent && ((FocusEvent) event).value) || event instanceof DropEvent ||
                event instanceof KeyEvent)) {
            final Object r = getRedirectFocus();
            if (r != null) {
                if (r instanceof Component)
                    ((Component) r).requestFocus();
                else if (r instanceof FileChooser)
                    ((FileChooser) r).requestFocus();
                return;
            }
        }
        final Class<? extends IEvent> c = event.getClass();
        for (final Map.Entry<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<?>>> e : directEventListeners.entrySet())
            if (c.isAssignableFrom(e.getKey()))
                for (final EventListener l : e.getValue())
                    l.run(event);
        if (event instanceof SingleEvent) {
            final Class<?> ec = event.getClass();
            for (final IEvent e : events)
                if (e.getClass().equals(ec)) {
                    final IEvent event1 = ((SingleEvent) event).combine((SingleEvent) e);
                    if (event1 == null || event1 == e)
                        return;
                    events.remove(e);
                    break;
                }
        }
        events.add(event);
        updated();
    }

    public void fireLater(final IEvent event) { invokeLater(() -> fire(event)); }

    public void repaint() { fire(new RepaintEvent(this)); }

    public void setVisible(final boolean visible) {
        synchronized (locker) {
            if (this.visible == visible)
                return;
            this.visible = visible;
        }
        fire(new VisibleEvent(this, visible));
    }

    public void setEnabled(final boolean enabled) {
        setPseudoClass("enabled", enabled);
        setPseudoClass("disabled", !enabled);
        fire(new EnableEvent(this, enabled));
    }

    private volatile boolean lastX = false, lastY = false;

    public void setStartX(final Point point) {
        lastX = false;
        startX.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setStartY(final Point point) {
        lastY = false;
        startY.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setEndX(final Point point) {
        lastX = true;
        endX.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setEndY(final Point point) {
        lastY = true;
        endY.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setX(final float x, final Unit unit) {
        lastX = false;
        if (unit == Unit.DP)
            startX.set(new NumberPointMultiplier(dp, x));
        else
            startX.set(new NumberPointAdd(x, null));
        fire(new ChangePointEvent(this));
    }

    public void setX(final float x) {
        lastX = false;
        startX.set(new NumberPointAdd(x, null));
        fire(new ChangePointEvent(this));
    }

    public void setY(final float y, final Unit unit) {
        lastY = false;
        if (unit == Unit.DP)
            startY.set(new NumberPointMultiplier(dp, y));
        else
            startY.set(new NumberPointAdd(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setY(final float y) {
        lastY = false;
        startY.set(new NumberPointAdd(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setLocation(final float x, final float y) {
        lastX = false;
        lastY = false;
        startX.set(new NumberPointAdd(x, null));
        startY.set(new NumberPointAdd(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setWidth(final Point width) {
        if (lastX)
            startX.set(new PPointSubtract(endX, width));
        else
            endX.set(new PPointAdd(startX, width));
        fire(new ChangePointEvent(this));
    }

    public void setWidth(final float width, final Unit unit) {
        if (lastX)
            startX.set(
                    unit == Unit.DP ? new PPointSubtract(endX, new NumberPointMultiplier(dp, width)) :
                            new NumberPointAdd(-width, endX)
            );
        else
            endX.set(
                    unit == Unit.DP ? new PPointAdd(startX, new NumberPointMultiplier(dp, width)) :
                            new NumberPointAdd(width, startX)
            );
        fire(new ChangePointEvent(this));
    }

    public void setWidth(final float width) {
        if (lastX)
            startX.set(new NumberPointAdd(-width, endX));
        else
            endX.set(new NumberPointAdd(width, startX));
        fire(new ChangePointEvent(this));
    }

    public void setHeight(final Point height) {
        if (lastY)
            startY.set(new PPointSubtract(endY, height));
        else
            endY.set(new PPointAdd(startY, height));
        fire(new ChangePointEvent(this));
    }

    public void setHeight(final float height, final Unit unit) {
        if (lastY)
            startY.set(unit == Unit.DP ? new PPointSubtract(endY, new NumberPointMultiplier(dp, height)) : new NumberPointAdd(-height, endY));
        else
            endY.set(unit == Unit.DP ? new PPointAdd(new NumberPointMultiplier(dp, height), startY) : new NumberPointAdd(height, startY));
        fire(new ChangePointEvent(this));
    }

    public void setHeight(final float height) {
        if (lastY)
            startY.set(new NumberPointAdd(-height, endY));
        else
            endY.set(new NumberPointAdd(height, startY));
        fire(new ChangePointEvent(this));
    }

    public void toFront() {
        final Container c = getParent();
        if (c == null)
            return;
        c.toFront(this);
    }

    public void setSize(final float width, final float height) {
        setSize(width, height, false);
    }

    private boolean aSet(final PointSet set, final float offset, final Point target) {
        final Point p = set.get();
        if (!(p instanceof NumberPointAdd))
            return true;
        final NumberPointAdd a = (NumberPointAdd) p;
        return a.getNumber() != offset || a.getPoint() != target;
    }

    @SuppressWarnings("AssignmentUsedAsCondition")
    public void setSize(final float width, final float height, final boolean isSystem) {
        final boolean x, y;
        if (lastX) {
            if (x = aSet(startX, -width, endX))
                startX.set(new NumberPointAdd(-width, endX));
        } else if (x = aSet(endX, width, startX))
            endX.set(new NumberPointAdd(width, startX));
        if (lastY) {
            if (y = aSet(startY, -height, endY))
                startY.set(new NumberPointAdd(-height, endY));
        } else if (y = aSet(endY, height, startY))
            endY.set(new NumberPointAdd(height, startY));
        if (x || y)
            fire(new ChangePointEvent(this, isSystem));
    }

    public void animate(final Curve curve) {

    }

    private static final List<String>
            outlineColorProperties = Arrays.asList("outline-color", "outline"),
            outlineWidthProperties = Arrays.asList("outline-width", "outline"),
            backgroundColorProperties = Arrays.asList("background-color", "background"),
            backgroundImageProperties = Arrays.asList("background-image", "background");
    public static final ThreadLocal<ArrayList<Object>> ss = ThreadLocal.withInitial(ArrayList::new);
    public void paint(final Context context) {
        final List<Object> ss = Component.ss.get();
        final float
                w = width.calcFloat(), h = height.calcFloat(),
                borderRadius, outlineWidth;
        final Paint borderColor;

        context.translate(offsetSX.calcFloat(), offsetSY.calcFloat());

        ss.clear();
        getSet(evalVar("border-radius"), ss, StyleProperty.numberFilter, 0);
        borderRadius = calc(ss, 0, Orientation.HORIZONTAL, 0);
        ss.clear();
        getSet(evalVar(outlineWidthProperties), ss, StyleProperty.numberFilter, 0);
        outlineWidth = calc(ss, 0, Orientation.HORIZONTAL, 0);
        ss.clear();
        getSet(evalVar(outlineColorProperties), ss, StyleProperty.paintFilter, 0);
        borderColor = getPaint(ss, 0, Color.TRANSPARENT);

        if (outlineWidth >= 0.5f && borderColor != null && (!(borderColor instanceof Color) || ((Color) borderColor).alpha > 0)) {
            context.setPaint(borderColor);

            if (borderRadius >= 0.5f) {
                final float offset = outlineWidth / 2;
                context.setStrokeWidth(outlineWidth);
                context.draw(context.newRoundShape(-offset, -offset, w + outlineWidth, h + outlineWidth, borderRadius + outlineWidth));
                context.setStrokeWidth(1);
            } else {
                context.drawRect(-outlineWidth, -outlineWidth, w + outlineWidth * 2, outlineWidth);
                context.drawRect(-outlineWidth, h, w + outlineWidth * 2, outlineWidth);
                context.drawRect(-outlineWidth, 0, outlineWidth, h);
                context.drawRect(w, 0, outlineWidth, h);
            }
        }

        if (borderRadius >= 0.5f)
            context.setClip(context.newRoundShape(0, 0, w, h, borderRadius));

        List<List<Object>> vars = evalVar(backgroundColorProperties);
        if (vars != null && !vars.isEmpty())
            for (final List<Object> set : vars) {
                ss.clear();
                StyleProperty.filter(set, ss, StyleProperty.paintFilter);
                if (!ss.isEmpty()) {
                    final Paint bg = getPaint(ss, 0, Color.TRANSPARENT);
                    if ((!(bg instanceof Color) || ((Color) bg).alpha > 0)) {
                        context.setPaint(bg);
                        context.drawRect(0, 0, w, h);
                    }
                    break;
                }
            }

        vars = evalVar(backgroundImageProperties);
        if (vars != null && !vars.isEmpty())
            for (final List<Object> set : vars) {
                ss.clear();
                StyleProperty.filter(set, ss, StyleProperty.imageFilter);
                if (!ss.isEmpty())
                    for (final Object image : ss) {
                        final Image img = getImage(image, null);
                        if (img != null)
                            context.drawImage(Cache.scale(img, w, h), 0, 0);
                    }
            }

        //final Image img = getImage("background-image");
        //if (img != null)
        //    context.drawImage(Cache.scale(img, w, h), 0, 0);
    }

    public Point getPoint(final String name, final Orientation orientation, final int set, final int index, final float defValue) {
        return getPoint(new String[] { name }, orientation, set, index, defValue);
    }

    public Point getPoint(final List<String> names, final Orientation orientation, final int set, final int index, final float defValue) {
        final String[] l = names.toArray(new String[0]);
        Arrays.sort(l);
        return getPoint(l, orientation, set, index, defValue);
    }

    public Point getPoint(final String[] names, final Orientation orientation, final int set, final int index, final float defValue) {
        return new Point() {
            private volatile float v;

            {
                onChange(getProperty(names));
            }

            public void onChange(final StyleProperty property) {
                final List<List<Object>> r = evalVar(property);
                final List<Object> ss = Component.ss.get();
                ss.clear();
                getSet(r, ss, StyleProperty.numberFilter, set);
                v = calc(ss, index, orientation, defValue);
                reset();
            }

            @Override
            public float calcFloat() {
                return v;
            }

            @Override
            public void onConstruct() {
                Component.this.subscribe(names, this::onChange);
                super.onConstruct();
            }

            @Override
            public void onDestruct() {
                Component.this.unsubscribe(this::onChange);
                super.onDestruct();
            }
        };
    }

    public static void getSet(final List<List<Object>> l, final List<Object> o, final Predicate<Object> filter, final int index) {
        if (l == null || index >= l.size())
            return;
        StyleProperty.filter(l.get(index), o, filter);
    }

    public static void getLayoutSet(final List<List<Object>> l, final List<Object> o, final int index) {
        if (l == null || index >= l.size())
            return;
        StyleProperty.filter(l.get(index), o, obj -> {
            if (obj instanceof String && ((String) obj).equalsIgnoreCase("auto"))
                return true;
            return StyleProperty.numberFilter.test(obj);
        });
    }

    public static <T extends Enum<T>> void getEnumSet(final List<List<Object>> l, final List<Object> o, final Class<T> e, final int index) {
        if (l == null || index >= l.size())
            return;
        StyleProperty.filterEnum(l.get(index), o, e);
    }

    public static Object get(final List<Object> l, final int index) {
        if (index >= l.size())
            return null;
        return l.get(index);
    }

    public float calc(final List<Object> l, final int index, final Orientation orientation, final float defValue) {
        if (index >= l.size())
            return defValue;
        return calc(l.get(index), orientation, defValue);
    }

    public float calc(final Object o, final Orientation orientation, final float defValue) {
        if (o instanceof StyleCall) {
            final StyleCall c = (StyleCall) o;
            if ("calc".equals(c.name)) {
                L.w("calc function isn't implemented!");
            } else
                L.w("Unknown function", c.name);
        } else if (o instanceof String) {
            final String s = (String) o;
            if (s.endsWith("deg"))
                return Float.parseFloat(s.substring(0, s.length() - 3));
            if (s.endsWith("%")) {
                final Container c = getParent();
                final float v = Float.parseFloat(s.substring(0, s.length() - 1));
                return c != null ?
                        v * (orientation == Orientation.HORIZONTAL ? width.calcFloat() : height.calcFloat()) : v;
            }
            if (s.endsWith("px"))
                return Float.parseFloat(s.substring(0, s.length() - 2));
            if (s.endsWith("dp"))
                return Float.parseFloat(s.substring(0, s.length() - 2)) * dp.calcFloat();
            if (s.endsWith("sp"))
                return Float.parseFloat(s.substring(0, s.length() - 2)) * sp.calcFloat();
            return Float.parseFloat(s);
        } else if (o != null)
            L.w("Unknown number type", o.getClass());
        return defValue;
    }

    public Paint getPaint(final List<Object> l, final int index, final Paint defValue) {
        if (index >= l.size())
            return defValue;
        return getPaint(l.get(index), defValue);
    }

    public Paint getPaint(final Object o, final Paint defValue) {
        if (o instanceof String)
            try {
                return Color.parse((String) o);
            } catch (final IllegalArgumentException ex) {
                L.w(ex);
            }
        return defValue;
    }

    public Image getImage(final Object o, final Image defValue) {
        if (o instanceof StyleCall) {
            final StyleCall c = (StyleCall) o;
            if ("url".equals(c.name) && !c.objs.isEmpty()) {
                final List<Object> l = c.objs.get(0);
                if (!l.isEmpty()) {
                    final Object url = l.get(0);
                    if (url instanceof StyleStr) {
                        final String s = ((StyleStr) url).value;
                        final AtomicReference<Image> ir = new AtomicReference<>();
                        {
                            final SoftReference<Image> ref = Cache.images.computeIfAbsent(s, ignored -> {
                                final Framework f = getFramework();
                                if (f != null)
                                    try {
                                        final Image img = f.getImage(s);
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
                                final Image r = ref.get();
                                if (r != null)
                                    return r;
                            }
                        }
                        Cache.images.compute(s, (ignored, ref) -> {
                            if (ref != null) {
                                final Image img = ref.get();
                                if (img != null) {
                                    ir.set(img);
                                    return ref;
                                }
                            }
                            final Framework f = getFramework();
                            if (f != null)
                                try {
                                    final Image img = f.getImage(s);
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

    public <T extends Enum<T>> T getEnum(final List<Object> l, final Class<T> e, final int index, final T defValue) {
        if (index >= l.size())
            return defValue;
        return getEnum(l.get(index), e, defValue);
    }

    public <T extends Enum<T>> T getEnum(final Object obj, final Class<T> e, final T defValue) {
        if (!(obj instanceof String))
            return defValue;
        try {
            return MiniUtil.enumValueOfIgnoreCase(e, ((String) obj).replace('-', '_'));
        } catch (final IllegalAccessException ignored) {
            return defValue;
        }
    }
}