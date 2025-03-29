package illa4257.i4Framework.base.graphics;

import illa4257.i4Framework.base.utils.Geom;

public interface IPath {
    float x();
    float y();

    void begin(final float x, final float y);
    default void begin() { begin(0, 0); }
    void lineTo(final float x, final float y);
    void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3);
    default void curveTo(final float x1, final float y1, final float x2, final float y2) { curveTo(x1, y1, x1, y1, x2, y2); }

    default void arcTo(final float x, final float y, double startAngle, final double angle) {
        float sx = x(), sy = y(), dx = x - sx, dy = y - sy;

        final double len = Math.sqrt(dx * dx + dy * dy) * angle, step = angle / len;
        if ((dx > 0 && dy > 0) || (dx < 0 && dy < 0)) {
            startAngle -= 1.57;
            for (double r = 0; r < angle; r += step)
                lineTo(sx + (float) Math.cos(-startAngle - r) * dx, y - (float) Math.sin(-startAngle - r) * dy);
        } else
            for (double r = 0; r < angle; r += step)
                lineTo(x - (float) Math.cos(startAngle + r) * dx, sy + (float) Math.sin(startAngle + r) * dy);
    }

    default void arc(final float startX, final float startY, final float radiusX, final float radiusY, double startAngle, final double angle) {
        int len = Geom.pxArcLength(Math.max(radiusX, radiusY), angle);
        if (len == 0)
            return;
        final float step = (float) angle / len;
        if (angle > 0) {
            for (; len >= 0; startAngle += step, len--)
                lineTo(startX + (float) Math.sin(startAngle) * radiusX, startY - (float) Math.cos(startAngle) * radiusY);
            return;
        }
        if (angle == 0)
            lineTo(startX + (float) Math.sin(startAngle) * radiusX, startY - (float) Math.cos(startAngle) * radiusY);
    }

    default void arc(final float startX, final float startY, final float radius, final double startAngle, final double angle) {
        arc(startX, startY, radius, radius, startAngle, angle);
    }

    void close();
}