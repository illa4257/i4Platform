package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.graphics.IPath;

import java.awt.geom.Path2D;

public class SwingPath implements IPath {
    public final Path2D path = new Path2D.Float();
    private float x, y;

    @Override public float x() { return x; }
    @Override public float y() { return y; }

    @Override
    public void begin(final float x, final float y) {
        path.moveTo(this.x = x, this.y = y);
    }

    @Override
    public void lineTo(final float x, final float y) {
        path.lineTo(Math.round(this.x = x), Math.round(this.y = y));
    }

    @Override
    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        path.curveTo(x1, y1, x2, y2, x = x3, y = y3);
    }

    @Override
    public void curveTo(final float x1, final float y1, final float x2, final float y2) {
        path.quadTo(x1, y1, x = x2, y = y2);
    }

    @Override
    public void close() {
        path.closePath();
    }
}