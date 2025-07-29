package illa4257.i4Framework.base.utils;

import java.util.ArrayList;

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

    public static int[] steps(final float radius) {
        final double d = hPI, s = d / radius;
        final ArrayList<Integer> ints = new ArrayList<>();
        for (double c = 0; c < d; c += s) {
            final int cur = (int) Math.round(Math.sin(c) * radius);
            while (ints.size() <= cur)
                ints.add(0);
            ints.add(ints.remove(ints.size() - 1) + 1);
        }
        final int[] r = new int[ints.size()];
        int i = 0;
        for (final int n : ints)
            r[i++] = n;
        return r;
    }
}