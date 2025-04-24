package illa4257.i4Framework.base.points;

public class PointSet extends Point {
    final Object locker = new Object();
    volatile Point point = null;

    public PointSet() { set(PointAttach.ZERO); }
    public PointSet(final Point point) { set(point); }

    @Override
    protected float calc() {
        final Point p = point;
        return p != null ? p.calc() : 0;
    }

    public Point get() { return point; }

    public void set(Point newPoint) {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.unsubscribe(this::reset);
            if (newPoint == null)
                newPoint = PointAttach.ZERO;
            point = newPoint;
            if (isConstructed())
                newPoint.subscribe(this::reset);
        }
        reset();
    }

    @Override
    public void onConstruct() {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.subscribe(this::reset);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.unsubscribe(this::reset);
        }
    }
}