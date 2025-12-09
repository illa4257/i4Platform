package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseMoveEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.events.mouse.MouseScrollEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScrollBar extends Component {
    public static class ScrollEvent extends Event {
        public final int oldValue, newValue, delta;

        public ScrollEvent(final Component component, final int oldValue, final int newValue) {
            super(component);
            this.oldValue = oldValue;
            this.newValue = newValue;
            delta = newValue - oldValue;
        }
    }

    private final AtomicBoolean u = new AtomicBoolean(true);
    private final Orientation orientation;
    private volatile int unitIncrement = 1, min = 0, max = 0, scroll = 0, thumbOffset = 0, thumbLength = 0;

    public ScrollBar() { this(Orientation.VERTICAL); }

    public ScrollBar(final Orientation orientation) {
        this.orientation = orientation;
        addEventListener(RecalculateEvent.class, event -> u.set(true));
        addEventListener(MouseScrollEvent.class, event -> {
            if (event.orientation != orientation)
                return;
            final int old = scroll, min = Math.min(this.min, this.max), max = Math.max(this.max, this.min);
            //noinspection NonAtomicOperationOnVolatileField
            scroll = Math.max(Math.min(scroll + event.scroll * unitIncrement, max), min);
            if (old == scroll)
                return;
            u.set(true);
            event.parentPrevent(true);
            fire(new ScrollEvent(this, old, scroll));
            repaint();
        });
        final ConcurrentHashMap<Integer, Integer> h = new ConcurrentHashMap<>();
        addEventListener(MouseDownEvent.class, e -> h.put(e.parentPrevent(true).id(), Math.round(orientation == Orientation.HORIZONTAL ? e.x() : e.y())));
        addEventListener(MouseUpEvent.class, e -> h.remove(e.parentPrevent(true).id()));
        addEventListener(MouseMoveEvent.class, e -> {
            final Integer p = h.get(e.id());
            if (p == null)
                return;
            e.parentPrevent(true);
            final int np = Math.round(orientation == Orientation.HORIZONTAL ? e.x() : e.y()), old = scroll;
            final float l = (orientation == Orientation.HORIZONTAL ? width : height).calcFloat();
            scroll = Math.max(min, Math.min(max, old + Math.round((np - p) / (l - thumbLength) * (max - min))));
            h.computeIfPresent(e.id(), (ignored, ignored1) -> np);
            fire(new ScrollEvent(this, old, scroll));
            repaint();
        });
    }

    public void setUnitIncrement(final int newValue) {
        synchronized (locker) {
            unitIncrement = newValue;
        }
    }

    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getScroll() { return scroll; }

    public void setMin(final int newValue) {
        synchronized (locker) {
            min = newValue;
            u.set(true);
        }
    }

    public void setMax(final int newValue) {
        synchronized (locker) {
            max = newValue;
            u.set(true);
        }
    }

    public void setScroll(final int newValue) {
        synchronized (locker) {
            scroll = newValue;
            u.set(true);
        }
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final Color thumbColor = getColor("thumb-color");
        if (thumbColor.alpha <= 0)
            return;
        if (u.getAndSet(false)) {
            final int len = Math.max(max, min) - Math.min(min, max);
            if (len <= 0) {
                thumbOffset = 0;
                thumbLength = orientation == Orientation.VERTICAL ? height.calcInt() : width.calcInt();
                return;
            }
            final float s = orientation == Orientation.VERTICAL ? height.calcFloat() : width.calcFloat();
            thumbLength = Math.round(s / (len + s) * s);
            thumbOffset = Math.round((s - thumbLength) / len * (scroll - Math.min(min, max)));
        }
        ctx.setColor(thumbColor);
        if (orientation == Orientation.VERTICAL)
            ctx.drawRect(0, thumbOffset, width.calcFloat(), thumbLength);
        else
            ctx.drawRect(thumbOffset, 0, thumbLength, height.calcFloat());
    }
}