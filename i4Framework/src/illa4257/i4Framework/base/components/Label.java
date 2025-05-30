package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.math.Vector2D;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;

import java.util.Objects;

public class Label extends Component {
    public static final String regExp = "\r\n|[\r\n]";

    private String text;
    private String[] lines;

    public Label() { text = null; lines = null; }
    public Label(final String text) { this.text = text; lines = text == null ? null : text.split(regExp); }

    @Override
    public void paint(final Context ctx) {
        if (width.calcFloat() <= 0)
            return;
        super.paint(ctx);
        final String[] ll;
        synchronized (locker) {
            ll = lines;
        }
        if (ll == null || ll.length == 0)
            return;
        final Color tc = getColor("color");
        if (tc.alpha <= 0)
            return;
        ctx.setColor(tc);
        final Vector2D[] v2d = new Vector2D[ll.length];
        float h = 0, y;
        for (int i = 0; i < ll.length; i++) {
            v2d[i] = ctx.bounds(ll[i]);
            h += v2d[i].y;
        }
        y = (height.calcFloat() - h) / 2;
        final HorizontalAlign align = getEnumValue("text-align", HorizontalAlign.class, HorizontalAlign.LEFT);
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
            final float x = align == HorizontalAlign.LEFT ? 0 :
                    align == HorizontalAlign.CENTER ? (width.calcFloat() - v2d[i].x) / 2 :
                    width.calcFloat() - v2d[i].x;
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
}