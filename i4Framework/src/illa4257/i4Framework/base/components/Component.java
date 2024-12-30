package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.EventListener;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;
import illa4257.i4Framework.base.points.*;
import illa4257.i4Utils.IDestructor;
import illa4257.i4Utils.lists.DynList;
import illa4257.i4Utils.lists.PagedTmpList;
import illa4257.i4Utils.logger.i4Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Component implements IDestructor {
    final Object locker = new Object();
    private final AtomicInteger linkNumber = new AtomicInteger(0);
    private final Runnable[] listeners;

    boolean visible = true;

    public final PointSet startX = new PointSet(), startY = new PointSet(), endX = new PointSet(), endY = new PointSet();
    public final Point width = new PPointSubtract(endX, startX), height = new PPointSubtract(endY, startY);

    private final DynList<Runnable> repeatedInvoke = new DynList<>(Runnable.class);
    private final PagedTmpList<Runnable> invoke = new PagedTmpList<>(Runnable.class);

    private final ConcurrentHashMap<Class<? extends Event>, ConcurrentLinkedQueue<EventListener<? extends Event>>>
        eventListeners = new ConcurrentHashMap<>(),
        directEventListeners = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

    public Component() {
        listeners = new Runnable[] {
                () -> fire(new RecalculateEvent())
        };
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
        fire(new ChangePointEvent(this));
    }

    public void setStartY(final Point point) {
        startY.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setEndX(final Point point) {
        endX.set(point);
        fire(new ChangePointEvent(this));
    }

    public void setEndY(final Point point) {
        endY.set(point);
        fire(new ChangePointEvent(this));
    }


    public void setX(final float x) {
        startX.set(new PointAttach(x, null));
        fire(new ChangePointEvent(this));
    }

    public void setY(final float y) {
        startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setLocation(final float x, final float y) {
        startX.set(new PointAttach(x, null));
        startY.set(new PointAttach(y, null));
        fire(new ChangePointEvent(this));
    }

    public void setWidth(final float width) {
        endX.set(new PointAttach(width, startX));
        fire(new ChangePointEvent(this));
    }

    public void setHeight(final float height) {
        endY.set(new PointAttach(height, startY));
        fire(new ChangePointEvent(this));
    }

    public void setSize(final float width, final float height) {
        final boolean x, y;
        if (x = aSet(endX, width, startX))
            endX.set(new PointAttach(width, startX));
        if (y = aSet(endY, height, startY))
            endY.set(new PointAttach(height, startY));
        if (x || y)
            fire(new ChangePointEvent(this));
    }

    public void paint(final Context context) {}
}