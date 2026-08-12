package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;

public class ProgressBar extends Component {
    public volatile float value = -1, max = 100;

    public ProgressBar() {
        setFocusable(false);
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        final PropIter ss = getPI().nextLayer().nextSet();
        ss.select("color", StyleProperty.paintFilter).nextLayer().nextSet();
        final Paint paint = ss.paint(Color.TRANSPARENT);
        if (paint == null || (paint instanceof Color && ((Color) paint).alpha <= 0))
            return;
        context.setPaint(paint);
        final float m = max, v = Math.min(value, m);
        if (v > 0)
            context.fillRect(0, 0, width.calcFloat() * (v / m), height.calcFloat());
    }
}