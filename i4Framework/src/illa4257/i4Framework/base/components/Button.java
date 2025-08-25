package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.IMoveableInputEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.math.Vector2D;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;

import java.util.Objects;

import static illa4257.i4Framework.base.math.HorizontalAlign.CENTER;
import static illa4257.i4Framework.base.math.HorizontalAlign.LEFT;

public class Button extends Component {
    private final Object textLocker = new Object();
    private volatile Object text;

    public Button() { this(null); }
    public Button(final Object text) {
        this.text = text;
        setFocusable(true);
        final EventListener<IMoveableInputEvent> ml = e -> {
            if (
                    e.x() < 0 || e.x() > width.calcFloat() ||
                            e.y() < 0 || e.y() > height.calcFloat()
            )
                return;
            fire(new ActionEvent());
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
            fire(new ChangeTextEvent(old, text));
        }
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final Object te = text;
        final Color c = getColor("color");
        if (te == null || c.alpha <= 0)
            return;
        final String t = String.valueOf(te);
        ctx.setColor(c);
        final Vector2D s = ctx.bounds(t);
        final HorizontalAlign a = getEnumValue("text-align", HorizontalAlign.class, LEFT);
        ctx.drawString(t, a == LEFT ? 0 :
                        a == CENTER ? (width.calcFloat() - s.x) / 2 :
                width.calcFloat() - s.x,
                (height.calcFloat() - s.y) / 2);
    }
}