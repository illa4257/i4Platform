package illa4257.i4Framework.base.curves;

import illa4257.i4Framework.base.utils.Geom;

public class SineCurveOut extends Curve {
    @Override
    public float calc(float x) {
        return (float) Math.sin(x * Geom.hPI);
    }
}