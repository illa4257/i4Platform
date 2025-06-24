package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.components.ChangeZ;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseMoveEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.PPointAdd;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.styling.Cursor;
import illa4257.i4Framework.base.styling.StyleSetting;

import static illa4257.i4Framework.base.styling.Cursor.*;

public class WindowFrame extends Container {
    public final Label titleBar = new Label();
    public final Component window;

    public boolean notHolding = true, floating = true, holding = false, ls = false, rs = false, ts = false, bs = false;
    public float mx, my, cx, cy, holdX1, holdY1, holdX2, holdY2, holdX3, holdY3;

    public final StyleSetting cursor = new StyleSetting(Cursor.DEFAULT);

    public WindowFrame(final Component window) {
        this.window = window;
        styles.put("cursor", cursor);
        add(window);
        add(titleBar);
        window.addEventListener(ChangeTextEvent.class, e -> {
            titleBar.setText(e.newValue);
            titleBar.repaint();
        });
        titleBar.setX(2, Unit.DP);
        titleBar.setY(2, Unit.DP);
        if (window instanceof Window) {
            final Window w = (Window) window;
            w.addEventListener(ChangeTextEvent.class, e -> titleBar.setText(e.newValue));
            titleBar.setText(w.getTitle());
        }
        titleBar.setWidth(window.width);
        titleBar.setHeight(32, Unit.DP);
        window.setStartX(titleBar.startX);
        window.setStartY(titleBar.endY);
        setWidth(new PPointAdd(new PPointAdd(window.width, titleBar.startX), titleBar.startX));
        setHeight(new PPointAdd(new PPointAdd(window.height, titleBar.endY), titleBar.startY));

        addEventListener(MouseDownEvent.class, e -> {
            holding = true;
            holdX1 = e.globalX();
            holdY1 = e.globalY();
            holdX2 = window.width.calcFloat();
            holdY2 = window.height.calcFloat();
            holdX3 = startX.calcFloat();
            holdY3 = startY.calcFloat();

            ls = e.x() < titleBar.startX.calcFloat();
            ts = e.y() < titleBar.startY.calcFloat();
            rs = e.x() >= titleBar.endX.calcFloat();
            bs = e.y() >= window.endY.calcFloat();
        });
        addEventListener(MouseMoveEvent.class, e -> {
            final Container p = getParent();
            if (holding) {
                if (ls && !(startX.get() instanceof PPointSubtract))
                    setX(Math.max(e.globalX() - holdX1 + holdX3, 0));
                if (ts && !(startY.get() instanceof PPointSubtract))
                    setY(Math.max(e.globalY() - holdY1 + holdY3, 0));

                if (rs) {
                    window.setWidth(Math.max(e.globalX() - holdX1 + holdX2, 16));
                    if (endX.calcFloat() >= p.width.calcFloat())
                        setStartX(new PPointSubtract(p.width, window.endX));
                } else if (ls)
                    window.setWidth(Math.max(holdX1 - e.globalX() + holdX2, 16));
                if (bs) {
                    window.setHeight(Math.max(e.globalY() - holdY1 + holdY2, 16));
                    if (endY.calcFloat() >= p.height.calcFloat())
                        setStartY(new PPointSubtract(p.height, window.endY));
                } else if (ts)
                    window.setHeight(Math.max(holdY1 - e.globalY() + holdY2, 16));
                return;
            }

            final boolean
                    lx = e.x() < titleBar.startX.calcFloat(), ty = e.y() < titleBar.startY.calcFloat(),
                    rx = e.x() >= titleBar.endX.calcFloat(), by = e.y() >= window.endY.calcFloat();

            final Cursor r =
                    lx && ty ? NW_RESIZE :
                    rx && by ? SE_RESIZE :
                    lx && by ? SW_RESIZE :
                    rx && ty ? NE_RESIZE :
                          lx ? W_RESIZE :
                          ty ? N_RESIZE :
                          rx ? E_RESIZE :
                          by ? S_RESIZE :
                            NWSE_RESIZE;
            if (cursor.cursor() != r)
                cursor.set(r);
        });
        addEventListener(MouseUpEvent.class, e -> holding = false);

        titleBar.addEventListener(MouseDownEvent.class, e -> {
            mx = e.globalX();
            my = e.globalY();
            cx = startX.calcFloat();
            cy = startY.calcFloat();
            notHolding = false;
            fire(new ChangeZ(0));
            repaint();
        });
        titleBar.addEventListener(MouseMoveEvent.class, e -> {
            if (notHolding)
                return;
            if (!floating)
                floating = true;
            final Container p = getParent();
            setLocation(
                    Math.min(Math.max(cx + e.globalX() - mx, 0), p.width.calcFloat() - width.calcFloat()),
                    Math.min(Math.max(cy + e.globalY() - my, 0), p.height.calcFloat() - height.calcFloat())
            );
        });
        titleBar.addEventListener(MouseUpEvent.class, e -> notHolding = true);
    }
}