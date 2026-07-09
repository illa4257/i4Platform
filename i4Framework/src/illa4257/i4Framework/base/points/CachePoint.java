package illa4257.i4Framework.base.points;

public class CachePoint extends ACachePoint {
    private final Object locker = new Object();
    private volatile Point point;

    public CachePoint(final Point point) {
        this.point = point;
    }

    public void setPoint(final Point newValue) {
        if (point == newValue)
            return;
        synchronized (locker) {
            final Point old = point;
            if (old == newValue)
                return;
            if (old != null)
                old.unsubscribe(this::reset);
            point = newValue;
            if (isConstructed() && newValue != null)
                newValue.subscribe(this::reset);
        }
    }

    @Override
    protected float calc() {
        final Point p = point;
        return p != null ? p.calcFloat() : 0;
    }

    @Override
    public void onConstruct() {
        synchronized (locker) {
            final Point p = point;
            if (p != null)
                p.subscribe(this::reset);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        synchronized (locker) {
            final Point p = point;
            if (p != null)
                p.unsubscribe(this::reset);
        }
        super.onDestruct();
    }
}