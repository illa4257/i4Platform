package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Window;

public interface FrameworkWindow {
    Framework getFramework();
    Window getWindow();
    void dispose();
}