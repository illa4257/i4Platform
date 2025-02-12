package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.graphics.IPath;

import java.awt.geom.Path2D;

public class SwingPath implements IPath {
    public final Path2D path = new Path2D.Float();

    @Override
    public void begin(final float x, final float y) {
        path.moveTo(x, y);
    }

    @Override
    public void lineTo(final float x, final float y) {
        path.lineTo(x, y);
    }

    @Override
    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        path.curveTo(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void close() {
        path.closePath();
    }
}