package illa4257.i4Framework.base.points.ops;

import illa4257.i4Framework.base.points.Point;

public class PPointMultiplier extends Point {
    private volatile Point point1, point2;

    public PPointMultiplier(final Point point1, final Point point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    public void setPoint1(final Point newValue) {
        Point old = point1;
        if (old != null)
            old.unsubscribe(this::reset);
        point1 = newValue;
        if (newValue != null && isConstructed())
            newValue.subscribe(this::reset);
    }

    public void setPoint2(final Point newValue) {
        Point old = point2;
        if (old != null)
            old.unsubscribe(this::reset);
        point2 = newValue;
        if (newValue != null && isConstructed())
            newValue.subscribe(this::reset);
    }

    @Override
    protected float calc() {
        final Point p1 = point1, p2 = point2;
        return p1 != null && p2 != null ? p1.calcFloat() * p2.calcFloat() : 0;
    }

    @Override
    public void onConstruct() {
        Point p = point1;
        if (p != null)
            p.subscribe(this::reset);
        p = point2;
        if (p != null)
            p.subscribe(this::reset);
    }

    @Override
    public void onDestruct() {
        Point p = point1;
        if (p != null)
            p.unsubscribe(this::reset);
        p = point2;
        if (p != null)
            p.unsubscribe(this::reset);
    }
}