package illa4257.i4Framework.base.graphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PathRecorder implements IPath {
    private float x, y;
    public final List<Consumer<IPath>> actions = new ArrayList<>();

    public void applyTo(final IPath path) {
        for (final Consumer<IPath> action : actions)
            action.accept(path);
    }

    @Override
    public float x() {
        return x;
    }

    @Override
    public float y() {
        return y;
    }

    @Override
    public void moveTo(float x, float y) {
        this.x = x;
        this.y = y;
        actions.add(p -> p.moveTo(x, y));
    }

    @Override
    public void lineTo(float x, float y) {
        this.x = x;
        this.y = y;
        actions.add(p -> p.lineTo(x, y));
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        this.x = x3;
        this.y = y3;
        actions.add(p -> p.curveTo(x1, y1, x2, y2, x3, y3));
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2) {
        this.x = x2;
        this.y = y2;
        actions.add(p -> p.curveTo(x1, y1, x2, y2));
    }

    @Override
    public void arcQ(float x, float y, float radius, boolean invert) {
        this.x = x;
        this.y = y;
        actions.add(p -> p.arcQ(x, y, radius, invert));
    }

    @Override
    public void arcTo(float x, float y, double angle, float cut) {
        this.x = x;
        this.y = y;
        actions.add(p -> p.arcTo(x, y, angle, cut));
    }

    @Override
    public void close() {
        actions.add(IPath::close);
    }
}
