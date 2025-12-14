package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.*;
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
import illa4257.i4Utils.media.Color;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.styling.StyleNumber;
import illa4257.i4Framework.base.styling.StyleSelector;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Framework.base.points.*;
import illa4257.i4Framework.base.styling.Cursor;
import illa4257.i4Utils.Destructor;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.lists.SwappableTmpQueue;
import illa4257.i4Utils.logger.i4Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public class Component extends Destructor {
    protected final Object locker = new Object();
    volatile boolean isFocusable = false, visible = true;

    public volatile Object redirectFocus = null;

    protected final SyncVar<Container> parent = new SyncVar<>();

    public final PointSet startX = new PointSet(), startY = new PointSet(), endX = new PointSet(), endY = new PointSet(),
            densityMultiplier = new PointSet(NumberPointConstant.ONE);
    public final Point width = new PPointSubtract(endX, startX), height = new PPointSubtract(endY, startY),
                    windowStartX = new PPointAdd(startX, null), windowStartY = new PPointAdd(startY, null),
                    windowEndX = new PPointAdd(endX, null), windowEndY = new PPointAdd(endY, null);

    protected final ConcurrentLinkedQueue<Runnable> repeatedInvoke = new ConcurrentLinkedQueue<>();
    protected final AtomicBoolean isRepeated = new AtomicBoolean(false);
    private final SwappableTmpQueue<Runnable> invoke = new SwappableTmpQueue<>();

    private final ConcurrentHashMap<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<? extends IEvent>>>
        eventListeners = new ConcurrentHashMap<>(),
        directEventListeners = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<IEvent> events = new ConcurrentLinkedQueue<>();

    public final SyncVar<String> id = new SyncVar<>(), tag = new SyncVar<>();
    public final ConcurrentLinkedQueue<String> classes = new ConcurrentLinkedQueue<>(), pseudoClasses = new ConcurrentLinkedQueue<>();
    public final ConcurrentHashMap<String, StyleSetting> styles = new ConcurrentHashMap<String, StyleSetting>() {
        @SuppressWarnings("NullableProblems")
        @Override
        public StyleSetting put(final String key, final StyleSetting value) {
            if (key == null || key.isEmpty() || value == null)
                return null;
            final Consumer<StyleSetting> c = subscribers.get(key);
            if (c != null)
                value.subscribed.offer(c);
            final StyleSetting old = super.put(key, value);
            if (old != null && c != null)
                old.subscribed.remove(c);
            if (c != null && old == getStyle(key))
                c.accept(value);
            return old;
        }
    };
    public final ConcurrentLinkedQueue<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> stylesheet = new ConcurrentLinkedQueue<>();

    private final ArrayList<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> cache = new ArrayList<>();

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Consumer<StyleSetting>>> subscribedProperties = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<StyleSetting>> subscribers = new ConcurrentHashMap<>();

    private final AtomicInteger lsx = new AtomicInteger(), lsy = new  AtomicInteger(),
            lex = new AtomicInteger(), ley = new  AtomicInteger();

    public Component() {
        Class<?> c = getClass();
        while ((c.isAnonymousClass() || c.isLocalClass()) && c.getSuperclass() != null)
            c = c.getSuperclass();
        tag.set(c.getSimpleName());
        addEventListener(ReCalcCheckEvent.class, e -> {
            final int sx = Float.floatToIntBits(startX.calcFloat()), sy = Float.floatToIntBits(startY.calcFloat()),
                    ex = Float.floatToIntBits(endX.calcFloat()), ey = Float.floatToIntBits(endY.calcFloat());
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
                for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> entry : cache)
                    for (final Map.Entry<String, StyleSetting> entry2 : entry.getValue().entrySet()) {
                        final Consumer<StyleSetting> cons = subscribers.get(entry2.getKey());
                        if (cons == null)
                            continue;
                        entry2.getValue().subscribed.remove(cons);
                    }
                cache.clear();
                cache.add(new AbstractMap.SimpleImmutableEntry<>(null, styles));
                final ArrayList<StyleSelector> selectors = new ArrayList<>();
                cacheStyles(this, selectors);
                final Framework framework = getFramework();
                if (framework != null)
                    cacheStyles(framework.stylesheet, selectors);
                for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> entry : cache)
                    for (final Map.Entry<String, StyleSetting> entry2 : entry.getValue().entrySet()) {
                        final Consumer<StyleSetting> cons = subscribers.get(entry2.getKey());
                        if (cons == null)
                            continue;
                        entry2.getValue().subscribed.offer(cons);
                    }
            }
            for (final Map.Entry<String, Consumer<StyleSetting>> cons : subscribers.entrySet())
                cons.getValue().accept(getStyle(cons.getKey()));
            repaint();
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
            final Queue<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> stylesheet,
            final ArrayList<StyleSelector> selectors) {
        int l = selectors.size();
        for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> e : stylesheet)
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
        final HashMap<String, StyleSetting> old = new HashMap<>();
        for (final String k : subscribers.keySet())
            old.put(k, getStyle(k));
        if (en)
            pseudoClasses.add(pseudoClass);
        else
            pseudoClasses.remove(pseudoClass);
        for (final Map.Entry<String, Consumer<StyleSetting>> e : subscribers.entrySet()) {
            final StyleSetting s = getStyle(e.getKey());
            if (old.get(e.getKey()) != s)
                e.getValue().accept(s);
        }
        repaint();
    }

    public StyleSetting getStyle(final String name) {
        synchronized (cache) {
            for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> e : cache) {
                if (e.getKey() != null && !e.getKey().pseudoClasses.stream().allMatch(s -> pseudoClasses.stream()
                        .anyMatch(s::equalsIgnoreCase)))
                    continue;
                final StyleSetting s = e.getValue().get(name);
                if (s != null)
                    return s;
            }
            return null;
        }
    }

    public void subscribe(final String name, final Consumer<StyleSetting> listener) {
        if (name == null || name.isEmpty() || listener == null)
            return;
        final ConcurrentLinkedQueue<Consumer<StyleSetting>> l =
                subscribedProperties.computeIfAbsent(name, ignored -> new ConcurrentLinkedQueue<>());
        if (!l.offer(listener))
            return;
        subscribers.computeIfAbsent(name, k -> {
            final Consumer<StyleSetting> cons = s -> {
                if (s == null || getStyle(k) != s)
                    return;
                l.forEach(c -> c.accept(s));
            };
            synchronized (cache) {
                for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> entry : cache) {
                    final StyleSetting s = entry.getValue().get(name);
                    if (s == null)
                        continue;
                    s.subscribed.offer(cons);
                }
            }
            return cons;
        });
    }

    public String getString(final String name, final String defaultValue) {
        final StyleSetting s = getStyle(name);
        if (s == null)
            return defaultValue;
        final String r = s.get(String.class);
        return r != null ? r : defaultValue;
    }

    public StyleNumber getNumber(final String name, final StyleNumber defaultValue) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.number(defaultValue) : defaultValue;
    }

    public float getFloat(final String name, final float defaultValue) {
        final StyleNumber n = getNumber(name, null);
        return n != null ? n.number : defaultValue;
    }

    public int getInt(final String name, final int defaultValue) {
        final StyleNumber n = getNumber(name, null);
        return n != null ? Math.round(n.unit == Unit.DP ? n.number * densityMultiplier.calcFloat() : n.number) : defaultValue;
    }

    public float calcStyleNumber(final String name, final Orientation orientation, final float defaultValue) {
        final StyleSetting s = getStyle(name);
        if (s == null)
            return defaultValue;
        final StyleNumber r = s.number(null);
        return r != null ? r.calc(this, orientation) : defaultValue;
    }

    public Color getColor(final String name, final Color defaultColor) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.color(defaultColor) : defaultColor;
    }

    public Color getColor(final String name) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.color(Color.TRANSPARENT) : Color.TRANSPARENT;
    }

    public Image getImage(final String name, final Image defaultImage) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.image(defaultImage) : defaultImage;
    }

    public Image getImage(final String name) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.image(null) : null;
    }

    public Cursor getCursor(final String name) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.cursor() : Cursor.DEFAULT;
    }

    public <T extends Enum<T>> T getEnumValue(final String name, final Class<T> tEnum, final T defaultValue) {
        final StyleSetting s = getStyle(name);
        return s != null ? s.enumValue(tEnum, defaultValue) : defaultValue;
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
        width.subscribe(recalcCheck);
        height.subscribe(recalcCheck);
    }

    @Override
    public void onDestruct() {
        width.unsubscribe(recalcCheck);
        height.unsubscribe(recalcCheck);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void invokeAll() {
        IEvent event;
        m:
        while ((event = events.poll()) != null) {
            final Class<? extends IEvent> c = event.getClass();
            for (final Map.Entry<Class<? extends IEvent>, ConcurrentLinkedQueue<EventListener<?>>> e : eventListeners.entrySet())
                if (c.isAssignableFrom(e.getKey()))
                    for (final EventListener l : e.getValue()) {
                        try {
                            l.run(event);
                        } catch (final Throwable ex) {
                            i4Logger.INSTANCE.log(ex);
                        }
                        if (event.isPrevented())
                            continue m;
                    }
            if (!event.isParentPrevented()) {
                final Container p = getParent();
                if (p == null)
                    continue;
                p.fire(event);
            }
        }

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
            startX.set(new NumberPointMultiplier(densityMultiplier, x));
        else
            startX.set(new PointAttach(x, null));
        fire(new ChangePointEvent(this));
    }

    public void setX(final float x) {
        lastX = false;
        startX.set(new PointAttach(x, null));
        fire(new ChangePointEvent(this));
    }

    public void setY(final float y, final Unit unit) {
        lastY = false;
        if (unit == Unit.DP)
            startY.set(new NumberPointMultiplier(densityMultiplier, y));
        else
            startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setY(final float y) {
        lastY = false;
        startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setLocation(final float x, final float y) {
        lastX = false;
        lastY = false;
        startX.set(new PointAttach(x, null));
        startY.set(new PointAttach(y, null));
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
                    unit == Unit.DP ? new PPointSubtract(endX, new NumberPointMultiplier(densityMultiplier, width)) :
                            new PointAttach(-width, endX)
            );
        else
            endX.set(
                    unit == Unit.DP ? new PPointAdd(startX, new NumberPointMultiplier(densityMultiplier, width)) :
                            new PointAttach(width, startX)
            );
        fire(new ChangePointEvent(this));
    }

    public void setWidth(final float width) {
        if (lastX)
            startX.set(new PointAttach(-width, endX));
        else
            endX.set(new PointAttach(width, startX));
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
            startY.set(unit == Unit.DP ? new PPointAdd(endY, new NumberPointMultiplier(densityMultiplier, height)) : new PointAttach(-height, endY));
        else
            endY.set(unit == Unit.DP ? new PPointAdd(new NumberPointMultiplier(densityMultiplier, height), startY) : new PointAttach(height, startY));
        fire(new ChangePointEvent(this));
    }

    public void setHeight(final float height) {
        if (lastY)
            startY.set(new PointAttach(-height, endY));
        else
            endY.set(new PointAttach(height, startY));
        fire(new ChangePointEvent(this));
    }

    public void toFront() {
        final Container c = getParent();
        if (c == null)
            return;
        if (c.components.remove(this))
            c.components.offer(this);
    }

    public void setSize(final float width, final float height) {
        setSize(width, height, false);
    }

    private boolean aSet(final PointSet set, final float offset, final Point target) {
        final Point p = set.get();
        if (!(p instanceof PointAttach))
            return true;
        final PointAttach a = (PointAttach) p;
        return a.value != offset || a.getPoint() != target;
    }

    @SuppressWarnings("AssignmentUsedAsCondition")
    public void setSize(final float width, final float height, final boolean isSystem) {
        final boolean x, y;
        if (lastX) {
            if (x = aSet(startX, -width, endX))
                startX.set(new PointAttach(-width, endX));
        } else if (x = aSet(endX, width, startX))
            endX.set(new PointAttach(width, startX));
        if (lastY) {
            if (y = aSet(startY, -height, endY))
                startY.set(new PointAttach(-height, endY));
        } else if (y = aSet(endY, height, startY))
            endY.set(new PointAttach(height, startY));
        if (x || y)
            fire(new ChangePointEvent(this, isSystem));
    }

    public void paint(final Context context) {
        final float
                w = width.calcFloat(), h = height.calcFloat(),
                borderRadius = calcStyleNumber("border-radius", Orientation.HORIZONTAL, 0),
                borderWidth = calcStyleNumber("border-width", Orientation.HORIZONTAL, 0);

        final Color borderColor = getColor("border-color"),
                    bg = getColor("background-color");

        if (borderWidth >= 0.5f && borderColor.alpha > 0) {
            context.setColor(borderColor);

            if (borderRadius >= 0.5f) {
                final float offset = borderWidth / 2;
                context.setStrokeWidth(borderWidth);
                context.draw(context.newRoundShape(-offset, -offset, w + borderWidth, h + borderWidth, borderRadius + borderWidth));
                context.setStrokeWidth(1);
            } else {
                context.drawRect(-borderWidth, -borderWidth, w + borderWidth * 2, borderWidth);
                context.drawRect(-borderWidth, h, w + borderWidth * 2, borderWidth);
                context.drawRect(-borderWidth, 0, borderWidth, h);
                context.drawRect(w, 0, borderWidth, h);
            }
        }

        if (borderRadius >= 0.5f)
            context.setClip(context.newRoundShape(0, 0, w, h, borderRadius));

        if (bg.alpha > 0) {
            context.setColor(bg);
            context.drawRect(0, 0, w, h);
        }

        final Image img = getImage("background-image");
        if (img != null)
            context.drawImage(img, 0, 0, w, h);
    }
}