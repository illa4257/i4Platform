package illa4257.i4Framework.android;

import android.graphics.Path;
import illa4257.i4Framework.base.graphics.IPath;

public class AndroidPath implements IPath {
    public final Path path = new Path();
    private float x = 0, y = 0;

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    @Override
    public void begin(float x, float y) {
        path.moveTo(this.x = x, this.y = y);
    }

    @Override
    public void lineTo(float x, float y) {
        path.lineTo(this.x = x, this.y = y);
    }

    @Override
    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        path.addArc(x1, y1, this.x = x2, this.y = y2, x3, y3);
    }

    @Override
    public void close() {
        path.close();
    }
}
