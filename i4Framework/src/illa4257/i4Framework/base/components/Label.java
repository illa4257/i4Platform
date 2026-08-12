package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.styling.HorizontalAlign;

import java.util.Objects;

import static illa4257.i4Framework.base.styling.HorizontalAlign.LEFT;

public class Label extends Component {
    public volatile Object text, font = null;
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
        final PropIter ss = getPI();

        ss.select("color", StyleProperty.paintFilter);
        Paint tc = ss.paint(Color.TRANSPARENT);
        if (tc == null || (tc instanceof Color && ((Color) tc).alpha <= 0))
            return;
        ctx.setPaint(tc);
        final Object f = font;
        if (f != null)
            ctx.setFont(font);
        final Vector2[] v2d = new Vector2[lines.length];
        float h = 0, y;
        for (int i = 0; i < lines.length; i++) {
            v2d[i] = ctx.bounds(lines[i]);
            h += v2d[i].y;
        }
        y = (height.calcFloat() - h) / 2;

        ss.select("text-align", HorizontalAlign.class).nextLayer().nextSet();
        final HorizontalAlign align = ss.e(HorizontalAlign.class, LEFT);
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