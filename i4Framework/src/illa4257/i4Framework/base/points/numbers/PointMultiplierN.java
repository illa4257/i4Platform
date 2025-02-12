package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.atomic.AtomicReference;

public class PointMultiplierN extends Point {
    private final SyncVar<Point> point = new SyncVar<>();
    private final AtomicReference<Float> number = new AtomicReference<>();

    public PointMultiplierN(final Point point, float number) {
        this.point.set(point);
        this.number.set(number);
    }

    public void setPoint(final Point newValue) {
        if (getLinkNumber() > 0)
            newValue.subscribe(this::reset);
        final Point old = point.getAndSet(newValue);
        if (old != null)
            old.unsubscribe(this::reset);
        if (old != newValue)
            reset();
    }

    public void setNumber(final float newValue) {
        number.set(newValue);
        reset();
    }

    @Override
    protected float calc() {
        final Point p = point.get();
        return p != null ? p.calcFloat() * number.get() : 0;
    }

    @Override
    public void onConstruct() {
        super.onConstruct();
        final Point p = point.get();
        if (p == null)
            return;
        p.subscribe(this::reset);
        reset();
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        final Point p = point.get();
        if (p != null)
            p.unsubscribe(this::reset);
    }
}