package illa4257.i4Framework.base.points;

public class PointSet extends Point {
    private final Object locker = new Object();
    private volatile Point point;

    public PointSet() { point = null; }
    public PointSet(final Point point) { this.point = point; }

    @Override
    public float calcFloat() {
        final Point p = point;
        return p != null ? p.calcFloat() : 0;
    }

    public Point get() { return point; }

    public void set(final Point newValue) {
        if (point == newValue)
            return;
        synchronized (locker) {
            final Point old = point;
            if (old == newValue)
                return;
            if (old != null)
                old.unsubscribe(reset);
            point = newValue;
            if (isConstructed() && newValue != null)
                newValue.subscribe(reset);
        }
        reset();
    }

    @Override
    public void onConstruct() {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.subscribe(reset);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        synchronized (locker) {
            final Point o = point;
            if (o != null)
                o.unsubscribe(reset);
        }
        super.onDestruct();
    }

    @Override
    public String toString() {
        return "PointSet(" + point + ")";
    }
}