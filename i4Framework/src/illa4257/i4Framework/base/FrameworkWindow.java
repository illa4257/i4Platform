package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;

public interface FrameworkWindow {
    Framework getFramework();
    Window getWindow();

    default Point getDensityMultiplier() { return NumberPointConstant.ONE; }

    void dispose();
}