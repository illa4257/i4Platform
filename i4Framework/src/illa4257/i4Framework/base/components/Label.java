package illa4257.i4Framework.base.components;

import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.math.HorizontalAlign;
import illa4257.i4Framework.base.math.Vector2D;

import java.util.Objects;

public class Label extends Component {
    public volatile Object text;
    private String old = null;
    private String[] lines = null;

    public Label() { text = null; }
    public Label(final Object text) { this.text = text; }

    @Override
    public void paint(final Context ctx) {
        if (width.calcFloat() <= 0)
            return;
        super.paint(ctx);
        final Object t = text;
        if (t == null)
            return;
        final String s = t.toString();
        if (!Objects.equals(s, old)) {
            old = s;
            lines = s.split("\r\n|\r|\n");
        }
        if (lines == null || lines.length == 0)
            return;
        final Color tc = getColor("color");
        if (tc.alpha <= 0)
            return;
        ctx.setColor(tc);
        final Vector2D[] v2d = new Vector2D[lines.length];
        float h = 0, y;
        for (int i = 0; i < lines.length; i++) {
            v2d[i] = ctx.bounds(lines[i]);
            h += v2d[i].y;
        }
        y = (height.calcFloat() - h) / 2;
        final HorizontalAlign align = getEnumValue("text-align", HorizontalAlign.class, HorizontalAlign.LEFT);
        m:
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
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
}