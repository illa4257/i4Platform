package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Utils.media.Color;

public class ProgressBar extends Component {
    public volatile float value = -1, max = 100;

    public ProgressBar() {
        setFocusable(false);
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);

        final Color col = getColor("color");
        if (col.alpha <= 0)
            return;
        context.setColor(col);
        final float m = max, v = Math.min(value, m);
        if (v > 0)
            context.drawRect(0, 0, width.calcFloat() * (v / m), height.calcFloat());
    }
}