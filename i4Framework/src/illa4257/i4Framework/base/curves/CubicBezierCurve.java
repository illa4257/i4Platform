package illa4257.i4Framework.base.curves;

public class CubicBezierCurve extends Curve {
    private final float ax, bx, cx;
    private final float ay, by, cy;

    public CubicBezierCurve(final float x1, final float y1, final float x2, final float y2) {
        this.cx = 3f * x1;
        this.bx = 3f * (x2 - x1) - cx;
        this.ax = 1f - cx - bx;
        this.cy = 3f * y1;
        this.by = 3f * (y2 - y1) - cy;
        this.ay = 1f - cy - by;
    }

    @Override
    public float calc(final float x) {
        if (x <= 0) return 0;
        if (x >= 1) return 1;

        final float t = solveX(x);
        return ((ay * t + by) * t + cy) * t;
    }

    private float solveX(float targetX) {
        float t = targetX;
        for (int i = 0; i < 8; i++) {
            final float currentX = ((ax * t + bx) * t + cx) * t - targetX;

            if (Math.abs(currentX) < 1e-6f)
                return t;

            final float derivative = (3f * ax * t + 2f * bx) * t + cx;

            if (Math.abs(derivative) < 1e-6f)
                break;

            t -= currentX / derivative;
        }

        float low = 0f, high = 1f;
        t = targetX;

        while (low < high) {
            final float currentX = ((ax * t + bx) * t + cx) * t;

            if (Math.abs(currentX - targetX) < 1e-5f)
                return t;

            if (targetX > currentX)
                low = t;
            else
                high = t;

            t = (high + low) / 2;
        }

        return t;
    }
}