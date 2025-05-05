package illa4257.i4Framework.desktop.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;

public class DwmAPI {
    static { Native.register("DwmAPI"); }

    public static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

    public static native void DwmSetWindowAttribute(final WinDef.HWND hwnd, final long dwAttribute, final Pointer pvAttribute, final long cbAttribute);
}