package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.mouse.MouseScrollEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;

public class ScrollBar extends Component {
    public static class ScrollEvent extends Event {
        public final int oldValue, newValue, delta;

        public ScrollEvent(final int oldValue, final int newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
            delta = newValue - oldValue;
        }
    }

    private final Orientation orientation;
    private int unitIncrement = 1, min = 0, max = 0, scroll = 0, thumbOffset = 0, thumbLength = 0;

    public ScrollBar() { this(Orientation.VERTICAL); }

    public ScrollBar(final Orientation orientation) {
        this.orientation = orientation;
        addEventListener(ChangePointEvent.class, event -> reCalc());
        addEventListener(MouseScrollEvent.class, event -> {
            if (event.orientation != orientation)
                return;
            final int old = scroll, min = Math.min(this.min, this.max), max = Math.max(this.max, this.min);
            scroll = Math.max(Math.min(scroll + event.scroll * unitIncrement, max), min);
            if (old == scroll)
                return;
            reCalc();
            event.parentPrevent(true);
            fire(new ScrollEvent(old, scroll));
            fire(new RepaintEvent());
        });
    }

    public void setUnitIncrement(final int newValue) {
        synchronized (locker) {
            unitIncrement = newValue;
        }
    }

    private void reCalc() {
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

    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getScroll() { return scroll; }

    public void setMin(final int newValue) {
        synchronized (locker) {
            min = newValue;
            reCalc();
        }
    }

    public void setMax(final int newValue) {
        synchronized (locker) {
            max = newValue;
            reCalc();
        }
    }

    public void setScroll(final int newValue) {
        synchronized (locker) {
            scroll = newValue;
            reCalc();
        }
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final Color thumbColor = getColor("thumb-color");
        if (thumbColor.alpha <= 0)
            return;
        ctx.setColor(thumbColor);
        if (orientation == Orientation.VERTICAL)
            ctx.drawRect(0, thumbOffset, width.calcFloat(), thumbLength);
        else
            ctx.drawRect(thumbOffset, 0, thumbLength, height.calcFloat());
    }
}