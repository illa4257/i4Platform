package illa4257.i4Framework.base.math;

import java.awt.geom.Rectangle2D;

@Deprecated
/// Use {@link illa4257.i4Utils.math.Vector2}
public class Vector2D {
    public final float x, y;

    /// Use {@link illa4257.i4Utils.math.Vector2}
    public Vector2D(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2D fromSize(final Rectangle2D rectangle2D) {
        return new Vector2D((float) rectangle2D.getWidth(), (float) rectangle2D.getHeight());
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof Vector2D) {
            final Vector2D v = (Vector2D) o;
            return x == v.x && y == v.y;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Vector2D{x=" + x + ", y=" + y + "}";
    }
}