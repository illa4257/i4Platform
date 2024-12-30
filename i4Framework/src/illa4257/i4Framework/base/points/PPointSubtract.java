package illa4257.i4Framework.base.points;

public class PPointSubtract extends Point {
    private Point point1 = null, point2 = null;

    public PPointSubtract(final Point point1, final Point point2) {
        setPoint1(point1);
        setPoint2(point2);
    }

    @Override
    protected float calc() {
        return (point1 != null ? point1.calcFloat() : 0) - (point2 != null ? point2.calcFloat() : 0);
    }

    public void setPoint1(final Point newPoint) {
        final Point o = point1;
        if (o != null)
            o.unsubscribe(this::reset);
        point1 = newPoint;
        if (newPoint != null && getLinkNumber() > 0)
            newPoint.subscribe(this::reset);
    }

    public void setPoint2(final Point newPoint) {
        final Point o = point2;
        if (o != null)
            o.unsubscribe(this::reset);
        point2 = newPoint;
        if (newPoint != null && getLinkNumber() > 0)
            newPoint.subscribe(this::reset);
    }

    @Override
    public void onConstruct() {
        Point p = point1;
        if (p != null)
            p.subscribe(this::reset);
        p = point2;
        if (p != null)
            p.subscribe(this::reset);
        super.onConstruct();
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
