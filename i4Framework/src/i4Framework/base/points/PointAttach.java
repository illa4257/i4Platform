package i4Framework.base.points;

public class PointAttach extends Point {
    public static final PointAttach ZERO = new PointAttach(0, null);

    public final float value;
    private Point point;

    public PointAttach(final float value, final Point point) {
        this.value = value;
        //this.point = point;
        setPoint(point);
    }

    public Point getPoint() { return point; }

    public void setPoint(final Point newPoint) {
        final Point o = point;
        if (o != null)
            o.unsubscribe(super::reset);
        point = newPoint;
        if (newPoint != null && getLinkNumber() > 0)
            newPoint.subscribe(super::reset);
    }

    @Override
    public void reset() {
        super.reset();
        final Point p = point;
        if (p != null)
            p.reset();
    }

    @Override
    protected float calc() {
        final Point p = point;
        return p == null ? value : value + p.calcFloat();
    }

    @Override
    public void onConstruct() {
        final Point p = point;
        if (p != null)
            p.subscribe(super::reset);
    }

    @Override
    public void onDestruct() {
        final Point p = point;
        if (p != null)
            p.unsubscribe(super::reset);
    }
}