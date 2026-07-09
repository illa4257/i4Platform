package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;

import java.util.concurrent.atomic.AtomicInteger;

public class NumberPoint extends Point {
    private final AtomicInteger number;

    public NumberPoint() { number = new AtomicInteger(); }
    public NumberPoint(final float number) { this.number = new AtomicInteger(Float.floatToRawIntBits(number)); }

    public void set(final float newValue) {
        final int r = Float.floatToRawIntBits(newValue);
        if (number.getAndSet(r) != r)
            reset();
    }

    public float get() { return Float.intBitsToFloat(number.get()); }
    @Override public float calcFloat() { return Float.intBitsToFloat(number.get()); }

    @Override
    public String toString() {
        return "Num(" + Float.intBitsToFloat(number.get()) + ")";
    }
}