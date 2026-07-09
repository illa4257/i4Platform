package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.IMoveableInputEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;

import java.util.List;
import java.util.Objects;

import static illa4257.i4Framework.base.math.HorizontalAlign.CENTER;
import static illa4257.i4Framework.base.math.HorizontalAlign.LEFT;

public class Button extends Component {
    private final Object textLocker = new Object();
    private volatile Object text;
    public volatile Object font = null;

    public Button() { this(null); }
    public Button(final Object text) {
        this.text = text;
        setFocusable(true);
        final EventListener<IMoveableInputEvent> ml = e -> {
            if (
                    !isEnabled() ||
                    e.x() < 0 || e.x() > width.calcFloat() ||
                            e.y() < 0 || e.y() > height.calcFloat()
            )
                return;
            fire(new ActionEvent(this, e.isSystem()));
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
        if (te == null)
            return;
        final List<Object> ss = Component.ss.get();

        ss.clear();
        getSet(evalVar("color"), ss, StyleProperty.paintFilter, 0);
        final Paint c = getPaint(ss, 0, Color.TRANSPARENT);
        if (c instanceof Color && ((Color) c).alpha <= 0)
            return;
        final String t = String.valueOf(te);
        ctx.setPaint(c);
        final Object f = font;
        if (f != null)
            ctx.setFont(f);
        final Vector2 s = ctx.bounds(t);

        ss.clear();
        getEnumSet(evalVar("text-align"), ss, HorizontalAlign.class, 0);
        final HorizontalAlign a = getEnum(ss, HorizontalAlign.class, 0, LEFT);
        ctx.drawString(t, a == LEFT ? 0 :
                        a == CENTER ? (width.calcFloat() - s.x) / 2 :
                width.calcFloat() - s.x,
                (height.calcFloat() - s.y) / 2);
    }
}