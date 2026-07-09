package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.IMoveableInputEvent;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Framework.base.graphics.Color;

import java.util.List;
import java.util.Objects;

import static illa4257.i4Framework.base.math.HorizontalAlign.CENTER;
import static illa4257.i4Framework.base.math.HorizontalAlign.LEFT;

public class CheckBox extends Component {
    private final Object textLocker = new Object();
    private volatile Object text;
    public volatile Object font = null;
    public volatile boolean value;

    public CheckBox() { this(false, null); }
    public CheckBox(final Object text) { this(false, text); }

    public CheckBox(final boolean value, final Object text) {
        this.value = value;
        this.text = text;
        setFocusable(true);
        final EventListener<IMoveableInputEvent> ml = e -> {
            if (
                    e.x() < 0 || e.x() > width.calcFloat() ||
                            e.y() < 0 || e.y() > height.calcFloat()
            )
                return;
            //noinspection NonAtomicOperationOnVolatileField
            this.value = !this.value;
            fire(new ActionEvent(this, e.isSystem()));
            repaint();
        };
        addEventListener(MouseUpEvent.class, ml::run);
        addEventListener(TouchUpEvent.class, ml::run);
    }

    public void setText(final Object text) {
        synchronized (textLocker) {
            if (Objects.equals(this.text, text))
                return;
            final Object old = this.text;
            this.text = text;
            fire(new ChangeTextEvent(this, old, text));
        }
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final Object te = text;
        final float offset = 4 * dp.calcFloat(), cbh = height.calcFloat() - offset * 2;
        final List<Object> ss = Component.ss.get();

        final boolean v = value;

        ss.clear();
        getSet(evalVar(v ? "--check-color" : "--check-background-color"), ss, StyleProperty.paintFilter, 0);
        final Paint cbg = getPaint(ss, 0, Color.TRANSPARENT);
        if (cbg != null && (!(cbg instanceof Color) || ((Color) cbg).alpha > 0)) {
            ctx.setPaint(cbg);
            ctx.drawRect(offset, offset, cbh, cbh);
        }

        if (!v && te == null)
            return;
        ss.clear();
        getSet(evalVar("color"), ss, StyleProperty.paintFilter, 0);
        final Paint c = getPaint(ss, 0, Color.TRANSPARENT);
        if (c == null || (c instanceof Color && ((Color) c).alpha <= 0))
            return;
        final String t = String.valueOf(te);
        ctx.setPaint(c);
        if (v) {
            ctx.drawLine(offset + cbh / 8, offset + cbh * .5f, offset + cbh / 3, offset + cbh * .8f);
            ctx.drawLine(offset + cbh / 3, offset + cbh * .8f, offset + cbh - cbh / 8, offset + cbh / 4);
        }
        if (te == null)
            return;
        final Object f = font;
        if (f != null)
            ctx.setFont(f);
        final Vector2 s = ctx.bounds(t);

        ss.clear();
        getEnumSet(evalVar("text-align"), ss, HorizontalAlign.class, 0);
        final HorizontalAlign a = getEnum(ss, HorizontalAlign.class, 0, LEFT);
        ctx.drawString(t, a == LEFT ? offset + cbh + offset :
                        a == CENTER ? (width.calcFloat() - s.x) / 2 :
                                width.calcFloat() - s.x,
                (height.calcFloat() - s.y) / 2);
    }
}