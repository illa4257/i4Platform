package illa4257.i4Framework.base.points.ops;

import illa4257.i4Framework.base.points.Point;

public class PPointSubtract extends Point {
    private final Object locker = new Object();
    private volatile Point point1, point2;

    public PPointSubtract(final Point point1, final Point point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    public void setPoint1(final Point newValue) {
        if (point1 == newValue)
            return;
        synchronized (locker) {
            final Point old = point1;
            if (old == newValue)
                return;
            if (old != null)
                old.unsubscribe(this::reset);
            point1 = newValue;
            if (isConstructed() && newValue != null)
                newValue.subscribe(this::reset);
        }
        reset();
    }

    public void setPoint2(final Point newValue) {
        if (point2 == newValue)
            return;
        synchronized (locker) {
            final Point old = point2;
            if (old == newValue)
                return;
            if (old != null)
                old.unsubscribe(this::reset);
            point2 = newValue;
            if (isConstructed() && newValue != null)
                newValue.subscribe(this::reset);
        }
        reset();
    }

    @Override
    public float calcFloat() {
        final Point p1 = point1, p2 = point2;
        return (p1 != null ? p1.calcFloat() : 0) - (p2 != null ? p2.calcFloat() : 0);
    }

    @Override
    public void onConstruct() {
        synchronized (locker) {
            final Point p1 = point1;
            if (p1 != null)
                p1.subscribe(this::reset);
            final Point p2 = point2;
            if (p2 != null)
                p2.subscribe(this::reset);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        synchronized (locker) {
            final Point p1 = point1;
            if (p1 != null)
                p1.unsubscribe(this::reset);
            final Point p2 = point2;
            if (p2 != null)
                p2.unsubscribe(this::reset);
        }
        super.onDestruct();
    }

    @Override
    public String toString() {
        return "PPointSub(" + point1 + " - " + point2 + ")";
    }
}