package illa4257.i4Framework.base.graphics;

import illa4257.i4Framework.base.utils.Geom;

import java.util.Arrays;

public interface IPath {
    float x();
    float y();

    void begin(final float x, final float y);
    default void begin() { begin(0, 0); }
    void lineTo(final float x, final float y);
    void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3);
    default void curveTo(final float x1, final float y1, final float x2, final float y2) { curveTo(x1, y1, x1, y1, x2, y2); }

    default void arcQ(final float x, final float y, final float radius, final boolean invert) {
        final int[] curve = Geom.steps(radius);
        final int mx = x > x() ? 1 : -1, my = y > y() ? 1 : -1;
        int i = 0,
                cx = Math.round(x()),
                cy = Math.round(y());

        for (; i < curve.length; i++) {
            if (invert) {
                cx += mx * curve[i];
            } else {
                cy += my * curve[i];
            }
            lineTo(cx, cy);
            if (invert) {
                cy += my * curve[curve.length - i - 1];
            } else {
                cx += mx * curve[curve.length - i - 1];
            }
            lineTo(cx, cy);
        }
    }

    /*default void arc(double cx, double cy, double radius, double startAngle, double arcAngle) {
        final int segments = (int) Math.round(radius * 2);
        final double angleStep = arcAngle / segments;
        for (int i = 0; i <= segments; i++) {
            final double theta = startAngle + i * angleStep,
                    x = cx + radius * Math.cos(Math.toRadians(theta)),
                    y = cy + radius * Math.sin(Math.toRadians(theta));
            lineTo((float) x, (float) y);
        }
    }*/

    void close();
}