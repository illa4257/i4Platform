package illa4257.i4Framework.base.curves;

public class SineCurve extends Curve {
    @Override
    public float calc(float x) {
        return (float) ((Math.cos(x * Math.PI) - 1) / -2);
    }
}