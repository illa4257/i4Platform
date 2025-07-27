package illa4257.i4Framework.base.graphics;

public interface IPath {
    float x();
    float y();

    void begin(final float x, final float y);
    default void begin() { begin(0, 0); }
    void lineTo(final float x, final float y);
    void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3);
    default void curveTo(final float x1, final float y1, final float x2, final float y2) { curveTo(x1, y1, x1, y1, x2, y2); }

    default void arcQ(final float x, final float y, final float radius, final int q) {
        final int r = Math.round(radius);
        final float s = (float) Math.sqrt(r);

        
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