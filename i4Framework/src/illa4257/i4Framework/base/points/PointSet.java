package illa4257.i4Framework.base.points;

public class PointSet extends Point {
    final Object locker = new Object();
    Point point = null;

    public PointSet() { set(PointAttach.ZERO); }
    public PointSet(final Point point) { set(point); }

    @Override
    protected float calc() {
        final Point p = point;
        if (p == null)
            return 0;
        return p.calc();
    }

    public Point get() { synchronized(locker) { return point; } }

    public void set(Point newPoint) {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.unsubscribe(this::reset);
            if (newPoint == null)
                newPoint = PointAttach.ZERO;
            point = newPoint;
            if (getLinkNumber() > 0)
                newPoint.subscribe(this::reset);
        }
        reset();
    }

    @Override
    public void onConstruct() {
        final Point o = point;
        if (o != null)
            o.subscribe(this::reset);
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        final Point o = point;
        if (o != null)
            o.unsubscribe(this::reset);
    }
}