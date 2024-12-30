package illa4257.i4Framework.base;

import java.awt.geom.Rectangle2D;

public class Vector2D {
    public final float x, y;

    public Vector2D(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    public static Vector2D fromSize(final Rectangle2D rectangle2D) {
        return new Vector2D((float) rectangle2D.getWidth(), (float) rectangle2D.getHeight());
    }
}