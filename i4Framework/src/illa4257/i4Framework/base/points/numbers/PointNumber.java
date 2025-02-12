package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;

import java.util.concurrent.atomic.AtomicReference;

public class PointNumber extends Point {
    private final AtomicReference<Float> number = new AtomicReference<>();

    public PointNumber() { number.set(0f); }
    public PointNumber(final float number) { this.number.set(number); }

    public void set(final float newValue) {
        number.set(newValue);
        reset();
    }

    public float get() { return number.get(); }

    @Override
    protected float calc() {
        return number.get();
    }
}