package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;

import java.util.concurrent.atomic.AtomicInteger;

public class NumberPointMultiplier extends Point {
    private final AtomicInteger number = new AtomicInteger();
    private volatile Point point;

    public NumberPointMultiplier(float number, final Point point) { this.number.set(Float.floatToRawIntBits(number)); this.point = point; }
    public NumberPointMultiplier(final Point point, float number) { this.point = point; this.number.set(Float.floatToRawIntBits(number)); }

    public void setPoint(final Point newValue) {
        if (point == newValue)
            return;
        synchronized (number) {
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

    public void setNumber(final float newValue) {
        final int r = Float.floatToRawIntBits(newValue);
        if (number.getAndSet(r) != r)
            reset();
    }

    @Override
    public float calcFloat() {
        final Point p = point;
        return p != null ? p.calcFloat() * Float.intBitsToFloat(number.get()) : 0;
    }

    @Override
    public void onConstruct() {
        synchronized (number) {
            final Point p = point;
            if (p != null)
                p.subscribe(reset);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        synchronized (number) {
            final Point p = point;
            if (p != null)
                p.unsubscribe(reset);
        }
        super.onDestruct();
    }

    @Override
    public String toString() {
        return "NPointMul(" + point + " * " + Float.intBitsToFloat(number.get()) + ")";
    }
}