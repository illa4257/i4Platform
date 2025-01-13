package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.HorizontalAlign;
import illa4257.i4Framework.base.Vector2D;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Utils.SyncVar;

import java.util.Objects;

public class Label extends Component {
    public static final String regExp = "\r\n|[\r\n]";

    private String text;
    private String[] lines;
    private final SyncVar<Color> foreground = new SyncVar<>(Color.BLACK);
    private final SyncVar<HorizontalAlign> horizontalAlign = new SyncVar<>(HorizontalAlign.CENTER);

    public Label() { text = null; lines = null; }
    public Label(final String text) { this.text = text; lines = text == null ? null : text.split(regExp); }

    @Override
    public void paint(final Context ctx) {
        super.paint(ctx);
        if (width.calcFloat() <= 0)
            return;
        final String[] ll;
        synchronized (locker) {
            ll = lines;
        }
        final Vector2D[] v2d = new Vector2D[ll.length];
        float h = 0, y;
        for (int i = 0; i < ll.length; i++) {
            v2d[i] = ctx.bounds(ll[i]);
            h += v2d[i].y;
        }
        ctx.setColor(foreground.get());
        y = (height.calcFloat() - h) / 2;
        m:
        for (int i = 0; i < ll.length; i++) {
            String l = ll[i];
            if (l.isEmpty()) {
                y += v2d[i].y;
                continue;
            }
            boolean d3 = v2d[i].x > width.calcFloat();
            while (v2d[i].x > width.calcFloat()) {
                l = l.substring(0, l.length() - 1);
                if (l.isEmpty())
                    continue m;
                v2d[i] = ctx.bounds(l + "...");
            }
            if (d3)
                l += "...";
            final float x = horizontalAlign.get() == HorizontalAlign.LEFT ? 0 : (width.calcFloat() - v2d[i].x) / 2;
            ctx.drawString(l, x, y);
            y += v2d[i].y;
        }
    }

    public void setText(final String text) {
        final String old;
        synchronized (locker) {
            if (Objects.equals(this.text, text))
                return;
            old = this.text;
            this.text = text;
            lines = text == null ? null : text.split(regExp);
        }
        fire(new ChangeTextEvent(old, text));
    }

    public void setHorizontalAlign(final HorizontalAlign align) {
        horizontalAlign.set(align);
    }

    public void setForeground(final Color newColor) {
        foreground.set(newColor);
    }
}