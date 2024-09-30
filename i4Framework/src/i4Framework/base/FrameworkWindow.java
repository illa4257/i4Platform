package i4Framework.base;

import i4Framework.base.components.Window;

public interface FrameworkWindow {
    Framework getFramework();
    Window getWindow();
    void dispose();
}