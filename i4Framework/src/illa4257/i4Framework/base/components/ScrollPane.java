package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.points.PointAttach;

public class ScrollPane extends Container {
    private int scrollBarWidth = 8;

    private Container c = null;
    public final ScrollBar vBar = new ScrollBar(), hBar = new ScrollBar(Orientation.HORIZONTAL);
    private boolean bv = true, bh = true;
    private int ow = -1, oh = -1;

    private static class ReCalcBars implements SingleEvent {}

    public ScrollPane() {
        vBar.setUnitIncrement(8);
        hBar.setUnitIncrement(8);

        vBar.setEndX(width);
        vBar.setStartX(new PointAttach(-scrollBarWidth, vBar.endX));
        vBar.setEndY(hBar.startY);
        add(vBar);

        hBar.setStartY(new PointAttach(-scrollBarWidth, hBar.endY));
        hBar.setEndX(vBar.startX);
        hBar.setEndY(height);
        add(hBar);

        addEventListener(ReCalcBars.class, e -> {
            if (c == null)
                return;
            final int cw = c.width.calcInt(), ch = c.height.calcInt();
            int w = width.calcInt(), h = height.calcInt();

            boolean nv = ch > h, nh;
            if (nv)
                w -= scrollBarWidth;
            nh = cw > w;
            if (nh) {
                h -= scrollBarWidth;
                if (!nv && (nv = ch > h))
                    w -= scrollBarWidth;
            }

            if (nv != bv)
                if (bv = nv)
                    vBar.setEndX(width);
                else
                    vBar.setEndX(new PointAttach(scrollBarWidth, width));

            if (nh != bh)
                if (bh = nh)
                    hBar.setEndY(height);
                else
                    hBar.setEndY(new PointAttach(scrollBarWidth, height));

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

        width.subscribe(() -> fire(new ReCalcBars()));
        height.subscribe(() -> fire(new ReCalcBars()));
    }

    private final EventListener<ChangePointEvent> l = e -> fire(new ReCalcBars());

    public void setContent(final Container container) {
        synchronized (locker) {
            if (c != null) {
                c.removeEventListener(l);
                remove(c);
            }
            if (add(c = container))
                container.addEventListener(ChangePointEvent.class, l);
            vBar.setScroll(0);
            hBar.setScroll(0);
            fire(new ReCalcBars());
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
}