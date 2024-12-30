package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;

public class Panel extends Container {
    private Color background = Color.repeat3(228);

    public void setBackground(final Color newColor) {
        synchronized (locker) {
            this.background = newColor;
        }
    }

    @Override public void paint(final Context ctx) {
        ctx.setColor(background);
        //ctx.drawRect(startX.calcFloat(), startY.calcFloat(), width.calcFloat(), height.calcFloat());
        ctx.drawRect(0, 0, width.calcFloat(), height.calcFloat());
    }
}