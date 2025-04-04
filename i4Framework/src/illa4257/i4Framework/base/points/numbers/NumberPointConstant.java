package illa4257.i4Framework.base.points.numbers;

import illa4257.i4Framework.base.points.Point;

public class NumberPointConstant extends Point {
    public static final NumberPointConstant
            ZERO = new NumberPointConstant(0),
            ONE = new NumberPointConstant(1);

    public final float value;

    public NumberPointConstant(final float value) { this.value = value; }

    @Override public boolean subscribe(final Runnable listener) { return false; }
    @Override public boolean unsubscribe(final Runnable listener) { return false; }
    @Override public void reset() {}
    @Override protected float calc() { return value; }
}