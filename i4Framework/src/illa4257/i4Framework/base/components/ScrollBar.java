package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.Orientation;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.mouse.MouseScrollEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;

public class ScrollBar extends Component {
    public static class ScrollEvent implements Event {
        public final int oldValue, newValue, delta;

        public ScrollEvent(final int oldValue, final int newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
            delta = newValue - oldValue;
        }
    }

    private Color background = Color.repeat3(160), thumbColor = Color.repeat3(192);
    private Orientation orientation;
    private int unitIncrement = 1, min = 0, max = 0, scroll = 0, thumbOffset = 0, thumbLength = 0;

    public ScrollBar() { this(Orientation.VERTICAL); }

    public ScrollBar(final Orientation orientation) {
        this.orientation = orientation;
        addEventListener(ChangePointEvent.class, event -> reCalc());
        addEventListener(MouseScrollEvent.class, event -> {
            final int old = scroll, min = Math.min(this.min, this.max), max = Math.max(this.max, this.min);
            scroll = Math.max(Math.min(scroll + event.scroll * unitIncrement, max), min);
            if (old == scroll)
                return;
            reCalc();
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
        /*final float l = (orientation == Orientation.VERTICAL ? height.calcFloat() : width.calcFloat()) / len;
        thumbLength = Math.round(l * unitIncrement);
        thumbOffset = Math.round(l * scroll);*/
        final float s = orientation == Orientation.VERTICAL ? height.calcFloat() : width.calcFloat();
        float l = s / len;
        //thumbLength = Math.round(l * unitIncrement);
        thumbLength = Math.round(s / l);
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
        ctx.setColor(background);
        ctx.drawRect(0, 0, width.calcFloat(), height.calcFloat());
        ctx.setColor(thumbColor);
        if (orientation == Orientation.VERTICAL)
            ctx.drawRect(0, thumbOffset, width.calcFloat(), thumbLength);
        else
            ctx.drawRect(thumbOffset, 0, thumbLength, height.calcFloat());
    }
}