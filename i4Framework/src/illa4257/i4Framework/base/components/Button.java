package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.math.Vector2D;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Utils.SyncVar;

import java.util.Objects;

import static illa4257.i4Framework.base.math.HorizontalAlign.CENTER;
import static illa4257.i4Framework.base.math.HorizontalAlign.LEFT;

public class Button extends Component {
    private final SyncVar<String> text;
    private final SyncVar<Color> foreground = new SyncVar<>(Color.BLACK);

    public Button() { this(null); }
    public Button(final String text) {
        this.text = new SyncVar<>(text);
        setFocusable(true);
        addEventListener(MouseUpEvent.class, e -> {
            if (
                    e.localX < 0 || e.localX > width.calcInt() ||
                    e.localY < 0 || e.localY > height.calcInt()
            )
                return;
            fire(new ActionEvent());
        });
    }

    public void setText(final String text) {
        final String old;
        if (!Objects.equals(old = this.text.getAndSet(text), text))
            fire(new ChangeTextEvent(old, text));
    }

    public void setForeground(final Color newColor) {
        foreground.set(newColor);
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final String t = text.get();
        final Color c = getColor("color");
        if (t == null || c.alpha <= 0)
            return;
        ctx.setColor(c);
        final Vector2D s = ctx.bounds(t);
        final HorizontalAlign a = getEnumValue("text-align", HorizontalAlign.class, LEFT);
        ctx.drawString(t, a == LEFT ? 0 :
                        a == CENTER ? (width.calcFloat() - s.x) / 2 :
                width.calcFloat() - s.x,
                (height.calcFloat() - s.y) / 2);
    }
}