package i4Framework.base.components;

import i4Framework.base.Color;
import i4Framework.base.Context;
import i4Framework.base.EventListener;
import i4Framework.base.Orientation;
import i4Framework.base.events.components.ChangePointEvent;
import i4Framework.base.points.PointAttach;

public class ScrollPane extends Container {
    private int scrollBarWidth = 8;
    private Color background = Color.repeat3(176);

    private Container c = null;
    public final ScrollBar vBar = new ScrollBar(), hBar = new ScrollBar(Orientation.HORIZONTAL);
    private boolean bv = true, bh = true;
    private int ow = -1, oh = -1;

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

        width.subscribe(this::reCalc);
        height.subscribe(this::reCalc);
    }

    private void reCalc() {
        if (c == null)
            return;
        final int cw = c.width.calcInt(), ch = c.height.calcInt();
        if (ow == cw && oh == ch)
            return;
        ow = cw;
        oh = ch;
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
        if (sh == 0)
            c.setY(0);
        if (sw == 0)
            c.setX(0);
    }

    private final EventListener<ChangePointEvent> l = e -> {
        /*final StackTraceElement[] s = Thread.currentThread().getStackTrace();
        invokeLater(() -> {
            Log.printStacktrace(s);
            this.reCalc();
        });*/
        this.reCalc();
    };

    public void setContent(final Container container) {
        synchronized (locker) {
            if (c != null) {
                c.removeEventListener(l);
                remove(c);
            }
            if (add(c = container))
                container.addEventListener(ChangePointEvent.class, l);
            reCalc();
            vBar.setScroll(0);
            hBar.setScroll(0);
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
    public void paint(final Context ctx) {
        final Color bg = background;
        if (bg == null)
            return;
        ctx.setColor(bg);
        ctx.drawRect(0, 0, width.calcFloat(), height.calcFloat());
    }
}