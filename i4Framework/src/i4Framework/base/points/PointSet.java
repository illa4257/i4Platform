package i4Framework.base.points;

public class PointSet extends Point {
    Point point = null;

    public PointSet() { this(PointAttach.ZERO); }

    public PointSet(final Point point) {
        //this.point = point;
        //point.subscribe(this::fireAll);
        set(point);
    }

    @Override
    protected float calc() {
        final Point p = point;
        if (p == null)
            return 0;
        return p.calc();
    }

    /*@Override
    public void reset() {
        final Point p = point;
        if (p != null)
            p.reset();
        else
            super.reset();
    }*/

    public Point get() { return point; }

    public void set(Point newPoint) {
        final Point o = point;
        if (o != null)
            o.unsubscribe(this::reset);
        if (newPoint == null)
            newPoint = PointAttach.ZERO;
        point = newPoint;
        if (getLinkNumber() > 0)
            newPoint.subscribe(this::reset);
        reset();
    }

    @Override
    public void onConstruct() {
        final Point o = point;
        if (o != null)
            o.subscribe(this::reset);
        reset();
    }

    @Override
    public void onDestruct() {
        final Point o = point;
        if (o != null)
            o.unsubscribe(this::reset);
    }
}