package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;

public class NumberPoint extends Point {
    private volatile float number;

    public NumberPoint() { number = 0f; }
    public NumberPoint(final float number) { this.number = number; }

    public void set(final float newValue) {
        number = newValue;
        reset();
    }

    public float get() { return number; }
    @Override protected float calc() { return number; }
}