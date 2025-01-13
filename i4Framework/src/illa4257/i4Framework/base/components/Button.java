package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.HorizontalAlign;
import illa4257.i4Framework.base.Vector2D;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.input.MouseUpEvent;
import illa4257.i4Utils.SyncVar;

import java.util.Objects;

public class Button extends Component {
    private final SyncVar<String> text;
    private final SyncVar<Color> foreground = new SyncVar<>(Color.BLACK);
    private final SyncVar<HorizontalAlign> horizontalAlign = new SyncVar<>(HorizontalAlign.CENTER);

    public Button() { this(null); }
    public Button(final String text) {
        this.text = new SyncVar<>(text);
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

    public void setHorizontalAlign(final HorizontalAlign align) {
        this.horizontalAlign.set(align);
    }

    public void setForeground(final Color newColor) {
        foreground.set(newColor);
    }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        final String t = text.get();
        if (t == null)
            return;
        final Vector2D s = ctx.bounds(t);
        ctx.setColor(foreground.get());
        ctx.drawString(t, horizontalAlign.get() == HorizontalAlign.LEFT ? 0 : (width.calcFloat() - s.x) / 2, (height.calcFloat() - s.y) / 2);
    }
}