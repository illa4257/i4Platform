package illa4257.i4Framework.base.utils;

public class Geom {
    /// Half PI
    public static final double hPI = Math.PI / 2d;

    public static int pxArcLength(double radius, double radians) {
        if (radians < 0)
            radians = -radians;
        if (radius < 0)
            radius = -radius;
        int r = 0;
        int oldX = 0, oldY = 0, x, y;
        for (; radians >= 0; radians -= .001) {
            x = (int) Math.round(Math.sin(radians) * radius);
            y = (int) Math.round(Math.cos(radians) * radius);
            if (oldX == x && oldY == y)
                continue;
            r++;
            oldX = x;
            oldY = y;
        }
        return r;
    }
}