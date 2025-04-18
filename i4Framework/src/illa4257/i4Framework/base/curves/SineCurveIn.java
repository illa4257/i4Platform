package illa4257.i4Framework.base.curves;

import illa4257.i4Framework.base.utils.Geom;

public class SineCurveIn extends Curve {
    @Override
    public float calc(float x) {
        return (float) -(Math.cos(x * Geom.hPI) - 1);
    }
}