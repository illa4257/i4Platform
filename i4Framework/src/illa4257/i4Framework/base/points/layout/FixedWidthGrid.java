package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Utils.Destructor;
import illa4257.i4Utils.SyncVar;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class FixedWidthGrid extends Destructor {
    private final AtomicReference<Float> spacing = new AtomicReference<>(0f);
    private final ArrayList<Component> components = new ArrayList<>();
    private final SyncVar<Point> width = new SyncVar<>();

    public void reset() {

    }

    public void setSpacing(final float spacing) {
        this.spacing.set(spacing);
    }

    public void setPoint(final Point point) {
        width.set(point);
    }

    public void add(final Component component) {
        components.add(component);
    }

    @Override
    public void onConstruct() {

    }

    @Override
    public void onDestruct() {

    }
}