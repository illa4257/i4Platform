package i4Framework.base.points;

public class PointSubtract extends Point {
    private Point p1, p2;

    public PointSubtract(final Point point1, final Point point2) {
        setPoint1(point1);
        setPoint2(point2);
    }

    public void setPoint1(final Point newPoint) {
        if (p1 == newPoint)
            return;
        final Point p = p1;
        if (p != null)
            p.unsubscribe(this::reset);
        p1 = newPoint;
        if (newPoint != null && getLinkNumber() > 0)
            newPoint.subscribe(this::reset);
        reset();
    }

    public void setPoint2(final Point newPoint) {
        if (p2 == newPoint)
            return;
        final Point p = p2;
        if (p != null)
            p.unsubscribe(this::reset);
        p2 = newPoint;
        if (newPoint != null && getLinkNumber() > 0)
            newPoint.subscribe(this::reset);
        reset();
    }

    @Override protected float calc() { return p1.calcFloat() - p2.calcFloat(); }

    @Override
    public void onConstruct() {
        Point o = p1;
        if (o != null)
            o.subscribe(this::reset);
        o = p2;
        if (o != null)
            o.subscribe(this::reset);
        reset();
    }

    @Override
    public void onDestruct() {
        Point o = p1;
        if (o != null)
            o.unsubscribe(this::reset);
        o = p2;
        if (o != null)
            o.unsubscribe(this::reset);
    }
}