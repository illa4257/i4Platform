package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.events.mouse.MouseScrollEvent;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPoint;

public class ScrollPane extends Container {
    private Container c = null;
    public final ScrollBar vBar = new ScrollBar(), hBar = new ScrollBar(Orientation.HORIZONTAL);
    private int ow = -1, oh = -1;

    private static class ReCalcBars extends Event implements SingleEvent {
        public ReCalcBars(final Component component) { super(component); }
    }
    private final Runnable reCalcRequest = () -> fire(new ReCalcBars(this));

    public final Point viewableWidth = new NumberPoint(), viewableHeight = new NumberPoint();

    public ScrollPane() {
        vBar.setUnitIncrement(8);
        hBar.setUnitIncrement(8);

        vBar.setEndX(width);
        vBar.setStartX(viewableWidth);
        vBar.setEndY(hBar.startY);
        add(vBar);

        hBar.setStartY(viewableHeight);
        hBar.setEndX(vBar.startX);
        hBar.setEndY(height);
        add(hBar);

        addEventListener(ReCalcBars.class, e -> {
            if (c == null)
                return;
            final int scrollBarWidth = getInt("--scrollbar-width", 0);
            if (scrollBarWidth <= 0)
                return;
            final int cw = c.width.calcInt(), ch = c.height.calcInt();
            int w = width.calcInt(), h = height.calcInt();

            ((NumberPoint) viewableWidth).set(w - scrollBarWidth);
            ((NumberPoint) viewableHeight).set(h - scrollBarWidth);

            boolean nv = ch > h;
            if (nv)
                w -= scrollBarWidth;
            if (cw > w) {
                h -= scrollBarWidth;
                if (!nv && ch > h)
                    w -= scrollBarWidth;
            }

            if (((NumberPoint) viewableWidth).get() != w)
                ((NumberPoint) viewableWidth).set(w);
            if (((NumberPoint) viewableHeight).get() != h)
                ((NumberPoint) viewableHeight).set(h);

            final int sh = Math.max(ch - h, 0), sw = Math.max(cw - w, 0);
            vBar.setMax(sh);
            hBar.setMax(sw);
            if (c == null)
                return;
            if (ow != cw || oh != ch) {
                if (sh == 0)
                    c.setY(0);
                if (sw == 0)
                    c.setX(0);
                ow = cw;
                oh = ch;
            }
            vBar.repaint();
            hBar.repaint();
        });

        vBar.addEventListener(ScrollBar.ScrollEvent.class, event -> {
            if (c == null)
                return;
            c.setY(-event.newValue);
        });
        hBar.addEventListener(ScrollBar.ScrollEvent.class, event -> {
            if (c == null)
                return;
            c.setX(-event.newValue);
        });

        addEventListener(StyleUpdateEvent.class, ignored -> fire(new ReCalcBars(this)));
        addEventListener(MouseScrollEvent.class, e -> {
            final ScrollBar bar = e.orientation == Orientation.VERTICAL ? vBar : hBar;
            if (
                    e.scroll == 0 ||
                    (bar.getMin() == bar.getScroll() && e.scroll < 0) ||
                    (bar.getMax() == bar.getScroll() && e.scroll > 0)
            )
                return;
            bar.fire(e);
        });
        addEventListener(RecalculateEvent.class, e -> fire(new ReCalcBars(this)));
    }

    private final EventListener<RecalculateEvent> l = e -> fire(new ReCalcBars(this));

    public void setContent(final Container container) {
        synchronized (locker) {
            if (c != null) {
                c.removeEventListener(l);
                remove(c);
            }
            if (add(c = container))
                container.addEventListener(RecalculateEvent.class, l);
            vBar.toFront();
            vBar.setScroll(0);
            hBar.toFront();
            hBar.setScroll(0);
            fire(new ReCalcBars(this));
        }
    }

    public void setScroll(final int x, final int y) {
        vBar.setScroll(y);
        hBar.setScroll(x);
        synchronized (getLocker()) {
            if (c == null)
                return;
            c.setX(x);
            c.setY(y);
        }
    }

    @Override
    public void onConstruct() {
        super.onConstruct();
        width.subscribe(reCalcRequest);
        densityMultiplier.subscribe(reCalcRequest);
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        densityMultiplier.unsubscribe(reCalcRequest);
    }
}