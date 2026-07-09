package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.StyleProperty;

import java.util.List;

public class ProgressBar extends Component {
    public volatile float value = -1, max = 100;

    public ProgressBar() {
        setFocusable(false);
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        final List<Object> ss = Component.ss.get();
        ss.clear();
        getSet(evalVar("color"), ss, StyleProperty.paintFilter, 0);
        final Paint paint = getPaint(ss, 0, Color.TRANSPARENT);
        if (paint == null || (paint instanceof Color && ((Color) paint).alpha <= 0))
            return;
        context.setPaint(paint);
        final float m = max, v = Math.min(value, m);
        if (v > 0)
            context.drawRect(0, 0, width.calcFloat() * (v / m), height.calcFloat());
    }
}