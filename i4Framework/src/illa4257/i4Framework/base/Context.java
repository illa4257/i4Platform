package illa4257.i4Framework.base;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.math.Vector2D;
import illa4257.i4Framework.base.utils.Geom;

public interface Context {
    default Context sub(final float x, final float y, final float w, final float h) { return this; }
    default void dispose() {}

    Object getClipI();
    Vector2D bounds(final String string);
    Vector2D bounds(final char[] string);

    void setColor(final Color color);
    void setStrokeWidth(final float newWidth);
    void setClipI(final Object clipArea);
    void setClip(final IPath path);

    IPath newPath();

    void drawLine(final float x1, final float y1, final float x2, final float y2);

    default void drawArc(final float x, final float y, final float radius, double startAngle, double angle) {
        final double step = 1 / (radius * 2);
        angle += startAngle + step;
        float ox = x + (float) Math.sin(startAngle) * radius, oy = y - (float) Math.cos(startAngle) * radius;
        if (startAngle < angle) {
            for (startAngle += step; startAngle < angle; startAngle += step)
                drawLine(ox, oy, ox = x + (float) Math.sin(startAngle) * radius, oy = y - (float) Math.cos(startAngle) * radius);
        } else
            for (startAngle -= step; startAngle > angle; startAngle -= step)
                drawLine(ox, oy, ox = x + (float) Math.sin(startAngle) * radius, oy = y - (float) Math.cos(startAngle) * radius);
    }

    default void drawArc(float x, float y, final float radiusFrom, float radiusTo, double startAngle, double angle) {
        final double step = angle / Geom.pxArcLength(radiusTo, angle);
        angle += startAngle;
        if (startAngle < angle) {
            for (; (float) startAngle <= (float) angle; startAngle += step)
                drawLine(x + (float) Math.sin(startAngle) * radiusFrom, y - (float) Math.cos(startAngle) * radiusFrom,
                        x + (float) Math.sin(startAngle) * radiusTo, y - (float) Math.cos(startAngle) * radiusTo);
        } else
            for (; (float) startAngle >= (float) angle; startAngle -= step)
                drawLine(x + (float) Math.sin(startAngle) * radiusFrom, y - (float) Math.cos(startAngle) * radiusFrom,
                        x + (float) Math.sin(startAngle) * radiusTo, y - (float) Math.cos(startAngle) * radiusTo);
    }

    void drawRect(final float x, final float y, final float w, final float h);
    void drawString(final String str, final float x, final float y);
    void drawString(final char[] str, final float x, final float y);

    void drawImage(final Image image, final float x, final float y, final float width, final float height);
}