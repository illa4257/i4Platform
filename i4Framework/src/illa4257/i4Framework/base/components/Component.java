package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.*;
import illa4257.i4Framework.base.EventListener;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.input.MouseEnterEvent;
import illa4257.i4Framework.base.events.input.MouseLeaveEvent;
import illa4257.i4Framework.base.events.input.MouseUpEvent;
import illa4257.i4Framework.base.points.*;
import illa4257.i4Utils.IDestructor;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.lists.DynList;
import illa4257.i4Utils.lists.PagedTmpList;
import illa4257.i4Utils.logger.i4Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Component implements IDestructor {
    protected final Object locker = new Object();
    boolean isFocused = false, isFocusable = false, isHovered = false;
    private final AtomicInteger linkNumber = new AtomicInteger(0);
    private final Runnable[] listeners;

    protected final SyncVar<Container> parent = new SyncVar<>();

    boolean visible = true;

    public final PointSet startX = new PointSet(), startY = new PointSet(), endX = new PointSet(), endY = new PointSet();
    public final Point width = new PPointSubtract(endX, startX), height = new PPointSubtract(endY, startY);

    private final DynList<Runnable> repeatedInvoke = new DynList<>(Runnable.class);
    private final PagedTmpList<Runnable> invoke = new PagedTmpList<>(Runnable.class);

    private final ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<? extends Event>>>
        eventListeners = new ConcurrentHashMap<>(),
        directEventListeners = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

    public final SyncVar<String> id = new SyncVar<>(), tag = new SyncVar<>();
    public final ConcurrentLinkedQueue<String> classes = new ConcurrentLinkedQueue<>();
    public final ConcurrentHashMap<String, StyleSetting> styles = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<StyleSelector, ConcurrentHashMap<String, StyleSetting>> stylesheet = new ConcurrentHashMap<>();

    public final ConcurrentLinkedQueue<String> pseudoClasses = new ConcurrentLinkedQueue<>();
    private final ArrayList<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> cache = new ArrayList<>();

    public Component() {
        Class<?> c = getClass();
        while ((c.isAnonymousClass() || c.isLocalClass()) && c.getSuperclass() != null)
            c = c.getSuperclass();
        tag.set(c.getSimpleName());
        listeners = new Runnable[] {
                () -> fire(new RecalculateEvent())
        };
        addEventListener(StyleUpdateEvent.class, e -> {
            synchronized (cache) {
                cache.clear();
                cache.add(new AbstractMap.SimpleImmutableEntry<>(null, styles));
                cacheStyles(this, new ArrayList<>());
            }
        });
        addEventListener(HoverEvent.class, e -> {
            synchronized (locker) {
                if (isHovered == e.value)
                    return;
                isHovered = e.value;
            }
            if (e.value)
                pseudoClasses.add("hover");
            else
                pseudoClasses.remove("hover");
            repaint();
        });
        addEventListener(FocusEvent.class, e -> {
            synchronized (locker) {
                if (isFocused == e.value)
                    return;
                isFocused = e.value;
            }
            if (e.value)
                pseudoClasses.add("focus");
            else
                pseudoClasses.remove("focus");
            repaint();
        });
        addEventListener(MouseEnterEvent.class, e -> fire(new HoverEvent(true)));
        addEventListener(MouseLeaveEvent.class, e -> fire(new HoverEvent(false)));
        fire(new StyleUpdateEvent());
    }

    private void cacheStyles(final Component c, final ArrayList<StyleSelector> selectors) {
        int l = selectors.size();
        for (final Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>> e : c.stylesheet.entrySet())
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

    public boolean isFocusable() {
        synchronized (locker) {
            return isFocusable;
        }
    }

    public boolean isFocused() {
        synchronized (locker) {
            return isFocused;
        }
    }

    private final ArrayList<EventListener<? extends Event>> focusListeners = new ArrayList<>();

    public void setFocusable(final boolean newValue) {
        synchronized (locker) {
            if (isFocusable == newValue)
                return;
            if (newValue) {
                focusListeners.add(addEventListener(MouseUpEvent.class, e -> requestFocus()));
            } else {
                removeEventListeners(focusListeners);
                focusListeners.clear();
            }
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

    @Override public int getLinkNumber() { return linkNumber.get(); }
    @Override public int addLinkNumber() { return linkNumber.incrementAndGet(); }
    @Override public int decLinkNumber() { return linkNumber.decrementAndGet(); }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void invokeAll() {
        Event event;
        while ((event = events.poll()) != null) {
            final Class<? extends Event> c = event.getClass();
            for (final Map.Entry<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> e : eventListeners.entrySet())
                if (c.isAssignableFrom(e.getKey()))
                    for (final EventListener l : e.getValue())
                        try {
                            l.run(event);
                        } catch (final Throwable ex) {
                            i4Logger.INSTANCE.log(ex);
                        }
        }
        invoke.nextPage();
        for (final Runnable r : invoke)
            try {
                r.run();
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
        repeatedInvoke.reset();
        Runnable r;
        while ((r = repeatedInvoke.next()) != null)
            try {
                r.run();
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    public void invokeLater(final Runnable runnable) { invoke.add(runnable); }

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
            l.wait();
        }
    }

    public void onTick(final Runnable runnable) { repeatedInvoke.add(runnable); }
    public void offTick(final Runnable runnable) { repeatedInvoke.remove(runnable); }

    private <T extends Event> EventListener<T> addEventListenerInternal(final Class<T> eventType, final EventListener<T> listener, final ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> listeners) {
        if (listeners.computeIfAbsent(eventType, t -> new ConcurrentLinkedQueue<>()).add(listener))
            return listener;
        return null;
    }

    public <T extends Event> EventListener<T> addEventListener(final Class<T> eventType, final EventListener<T> listener) {
        return addEventListenerInternal(eventType, listener, eventListeners);
    }

    public <T extends Event> EventListener<T> addDirectEventListener(final Class<T> eventType, final EventListener<T> listener) {
        return addEventListenerInternal(eventType, listener, directEventListeners);
    }

    public <T extends Event> boolean removeEventListener(final EventListener<T> listener) {
        for (final Map.Entry<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> e : eventListeners.entrySet())
            if (e.getValue().remove(listener))
                return true;
        return false;
    }

    public boolean removeEventListeners(final Collection<EventListener<? extends Event>> listeners) {
        for (final Map.Entry<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> e : eventListeners.entrySet())
            if (e.getValue().removeAll(listeners))
                return true;
        return false;
    }

    public <T extends Event> boolean removeDirectEventListener(final EventListener<T> listener) {
        for (final Map.Entry<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> e : directEventListeners.entrySet())
            if (e.getValue().remove(listener))
                return true;
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void fire(Event event) {
        //System.out.println(this + " / " + event);
        if (event == null)
            return;
        final Class<? extends Event> c = event.getClass();
        for (final Map.Entry<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<?>>> e : directEventListeners.entrySet())
            if (c.isAssignableFrom(e.getKey()))
                for (final EventListener l : e.getValue())
                    l.run(event);
        if (event instanceof SingleEvent) {
            final Class<?> ec = event.getClass();
            for (final Event e : events)
                if (e.getClass().equals(ec)) {
                    final Event event1 = ((SingleEvent) event).combine((SingleEvent) e);
                    if (event1 == null || event1 == e)
                        return;
                    events.remove(e);
                    break;
                }
        }
        events.add(event);
    }

    public void fireLater(final Event event) { invokeLater(() -> fire(event)); }

    public void repaint() { fire(new RepaintEvent()); }

    public void setVisible(final boolean visible) {
        synchronized (locker) {
            if (this.visible == visible)
                return;
            this.visible = visible;
        }
        fire(new VisibleEvent(visible));
    }

    public boolean isVisible() {
        synchronized (locker) {
            return visible;
        }
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


    public void setX(final float x) {
        startX.set(new PointAttach(x, null));
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

    public void setWidth(final float width) {
        endX.set(new PointAttach(width, startX));
        fire(new ChangePointEvent());
    }

    public void setHeight(final float height) {
        endY.set(new PointAttach(height, startY));
        fire(new ChangePointEvent());
    }

    public void setSize(final float width, final float height) {
        final boolean x, y;
        if (x = aSet(endX, width, startX))
            endX.set(new PointAttach(width, startX));
        if (y = aSet(endY, height, startY))
            endY.set(new PointAttach(height, startY));
        if (x || y)
            fire(new ChangePointEvent());
    }

    public void paint(final Context context) {
        final Color bg = getColor("background-color");
        if (bg.alpha > 0) {
            context.setColor(bg);
            context.drawRect(0, 0, width.calcFloat(), height.calcFloat());
        }
        final Image img = getImage("background-image");
        if (img != null)
            context.drawImage(img, 0, 0, width.calcFloat(), height.calcFloat());
    }
}