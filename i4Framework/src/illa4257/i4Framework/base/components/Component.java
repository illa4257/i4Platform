package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.*;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.IEvent;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseEnterEvent;
import illa4257.i4Framework.base.events.mouse.MouseLeaveEvent;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.base.utils.Geom;
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
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public class Component extends Destructor {
    protected final Object locker = new Object();
    volatile boolean isFocusable = false, visible = true;
    private final Runnable[] listeners;

    protected final SyncVar<Container> parent = new SyncVar<>();

    public final PointSet startX = new PointSet(), startY = new PointSet(), endX = new PointSet(), endY = new PointSet(),
            densityMultiplier = new PointSet(NumberPointConstant.ONE);
    public final Point width = new PPointSubtract(endX, startX), height = new PPointSubtract(endY, startY);

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

    public Component() {
        Class<?> c = getClass();
        while ((c.isAnonymousClass() || c.isLocalClass()) && c.getSuperclass() != null)
            c = c.getSuperclass();
        tag.set(c.getSimpleName());
        listeners = new Runnable[] {
                () -> fire(new RecalculateEvent())
        };
        addEventListener(ChangeParentEvent.class, e -> fire(new StyleUpdateEvent()));
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
            repaint();
        });
        addEventListener(HoverEvent.class, e -> setPseudoClass("hover", e.value));
        addEventListener(FocusEvent.class, e -> setPseudoClass("focus", e.value));
        addEventListener(MouseEnterEvent.class, e -> fire(new HoverEvent(true)));
        addEventListener(MouseLeaveEvent.class, e -> fire(new HoverEvent(false)));
    }

    public boolean isVisible() { return visible; }
    public boolean isFocusable() { return isFocusable; }
    public boolean isFocused() { return pseudoClasses.contains("focus"); }
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

        return true;
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
                if (getStyle(k) == s)
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
                focusListeners.add(addEventListener(MouseDownEvent.class, e -> requestFocus()));
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

    @Override
    public void onConstruct() {
        width.subscribe(listeners[0]);
        height.subscribe(listeners[0]);
    }

    @Override
    public void onDestruct() {
        width.unsubscribe(listeners[0]);
        height.unsubscribe(listeners[0]);
    }

    public Object getLocker() { return locker; }
    public Container getParent() { return parent.get(); }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void fire(final IEvent event) {
        if (event == null)
            return;
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

    public void repaint() { fire(new RepaintEvent()); }

    public void setVisible(final boolean visible) {
        synchronized (locker) {
            if (this.visible == visible)
                return;
            this.visible = visible;
        }
        fire(new VisibleEvent(visible));
    }

    private boolean aSet(final PointSet set, final float offset, final Point target) {
        final Point p = set.get();
        if (!(p instanceof PointAttach))
            return true;
        final PointAttach a = (PointAttach) p;
        return a.value != offset || a.getPoint() != target;
    }

    public void setStartX(final Point point) {
        startX.set(point);
        fire(new ChangePointEvent());
    }

    public void setStartY(final Point point) {
        startY.set(point);
        fire(new ChangePointEvent());
    }

    public void setEndX(final Point point) {
        endX.set(point);
        fire(new ChangePointEvent());
    }

    public void setEndY(final Point point) {
        endY.set(point);
        fire(new ChangePointEvent());
    }

    public void setX(final float x, final Unit unit) {
        if (unit == Unit.DP)
            startX.set(new NumberPointMultiplier(densityMultiplier, x));
        else
            startX.set(new PointAttach(x, null));
        fire(new ChangePointEvent());
    }

    public void setX(final float x) {
        startX.set(new PointAttach(x, null));
        fire(new ChangePointEvent());
    }

    public void setY(final float y, final Unit unit) {
        if (unit == Unit.DP)
            startY.set(new NumberPointMultiplier(densityMultiplier, y));
        else
            startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent());
    }

    public void setY(final float y) {
        startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent());
    }

    public void setLocation(final float x, final float y) {
        startX.set(new PointAttach(x, null));
        startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent());
    }

    public void setWidth(final Point width) {
        endX.set(new PPointAdd(startX, width));
        fire(new ChangePointEvent());
    }

    public void setWidth(final float width, final Unit unit) {
        if (unit == Unit.DP)
            endX.set(new PPointAdd(new NumberPointMultiplier(densityMultiplier, width), startX));
        else
            endX.set(new PointAttach(width, startX));
        fire(new ChangePointEvent());
    }

    public void setWidth(final float width) {
        endX.set(new PointAttach(width, startX));
        fire(new ChangePointEvent());
    }

    public void setHeight(final Point height) {
        endY.set(new PPointAdd(startY, height));
        fire(new ChangePointEvent());
    }

    public void setHeight(final float height, final Unit unit) {
        if (unit == Unit.DP)
            endY.set(new PPointAdd(new NumberPointMultiplier(densityMultiplier, height), startY));
        else
            endY.set(new PointAttach(height, startY));
        fire(new ChangePointEvent());
    }

    public void setHeight(final float height) {
        endY.set(new PointAttach(height, startY));
        fire(new ChangePointEvent());
    }

    public void setSize(final float width, final float height) {
        setSize(width, height, false);
    }

    @SuppressWarnings("AssignmentUsedAsCondition")
    public void setSize(final float width, final float height, final boolean isSystem) {
        final boolean x, y;
        if (x = aSet(endX, width, startX))
            endX.set(new PointAttach(width, startX));
        if (y = aSet(endY, height, startY))
            endY.set(new PointAttach(height, startY));
        if (x || y)
            fire(new ChangePointEvent(isSystem));
    }

    public void paint(final Context context) {
        final float
                w = width.calcFloat(), h = height.calcFloat(),
                borderRadius = calcStyleNumber("border-radius", Orientation.HORIZONTAL, 0),
                borderWidth = calcStyleNumber("border-width", Orientation.HORIZONTAL, 0);

        final Color borderColor = getColor("border-color"),
                    bg = getColor("background-color");

        if (borderWidth >= 0.5f && borderColor.alpha > 0) {
            final Object o = context.getClipI();
            context.setClipI(null);

            context.setColor(borderColor);

            if (borderRadius >= 0.5f) {
                final float offset = Math.round(borderWidth / 2);

                context.setStrokeWidth(borderWidth);

                final IPath p = context.newPath();
                p.begin(borderRadius - offset, -offset); // Start top line
                p.lineTo(w - borderRadius, -offset); // top line
                p.arcTo(w, borderRadius - offset, Geom.hPI, 0); // top right
                p.lineTo(w, h - borderRadius); // right line
                p.arcTo(w - borderRadius, h, Geom.hPI, 0); // bottom right
                p.lineTo(borderRadius - offset, h); // bottom line
                p.arcTo(-offset, h - borderRadius, Geom.hPI, 0); // bottom left
                p.lineTo(-offset, borderRadius - offset); // left line
                p.arcTo(borderRadius - offset, -offset, Geom.hPI, 0); // top left
                context.draw(p);

                context.setStrokeWidth(1);
            } else {
                context.drawRect(-borderWidth, -borderWidth, w + borderWidth * 2, borderWidth);
                context.drawRect(-borderWidth, h, w + borderWidth * 2, borderWidth);
                context.drawRect(-borderWidth, 0, borderWidth, h);
                context.drawRect(w, 0, borderWidth, h);
            }

            context.setClipI(o);
        }

        if (borderRadius >= 0.5f) {
            final IPath p = context.newPath();
            final float ew = w - borderRadius, eh = h - borderRadius;

            p.begin(borderRadius, 0);
            p.lineTo(ew, 0); // Top Line
            p.arcTo(w, borderRadius, Geom.hPI, borderRadius);
            p.lineTo(w, eh); // Right line
            p.arcTo(ew, h, Geom.hPI, borderRadius);
            p.lineTo(borderRadius, h); // Bottom line
            p.arcTo(0, eh, Geom.hPI, borderRadius);
            p.lineTo(0, borderRadius); // Left line
            p.arcTo(borderRadius, 0, Geom.hPI, borderRadius);

            p.close();
            context.setClip(p);
        }

        if (bg.alpha > 0) {
            context.setColor(bg);
            context.drawRect(0, 0, w, h);
        }

        final Image img = getImage("background-image");
        if (img != null)
            context.drawImage(img, 0, 0, w, h);
    }
}