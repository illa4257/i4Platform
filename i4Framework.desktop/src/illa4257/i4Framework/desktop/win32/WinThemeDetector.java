package illa4257.i4Framework.desktop.win32;

import com.sun.jna.platform.win32.*;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.desktop.DesktopFramework;

public class WinThemeDetector extends Thread {
    public final DesktopFramework framework;

    private static final String
            REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            REGISTRY_KEY = "AppsUseLightTheme";

    private boolean lastValue = false;

    public WinThemeDetector(final DesktopFramework framework) {
        this.framework = framework;
        setName("Theme Detector (Windows)");
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);

        //noinspection AssignmentUsedAsCondition
        if (lastValue = isDark())
            framework.onSystemThemeChange("dark", BaseTheme.DARK);
    }

    public boolean isDark() {
        return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY) &&
                Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_KEY) == 0;
    }

    @Override
    public void run() {
        final WinReg.HKEYByReference hKey = new WinReg.HKEYByReference();
        int err = Advapi32.INSTANCE.RegOpenKeyEx(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, 0, WinNT.KEY_READ, hKey);
        if (err != W32Errors.ERROR_SUCCESS)
            throw new Win32Exception(err);

        while (!isInterrupted()) {
            err = Advapi32.INSTANCE.RegNotifyChangeKeyValue(hKey.getValue(), false, WinNT.REG_NOTIFY_CHANGE_LAST_SET, null, false);
            if (err != W32Errors.ERROR_SUCCESS)
                throw new Win32Exception(err);

            final boolean v = isDark();
            if (v != lastValue) {
                lastValue = v;
                framework.onSystemThemeChange(v ? "dark" : "light", v ? BaseTheme.DARK : BaseTheme.LIGHT);
            }
        }
        Advapi32Util.registryCloseKey(hKey.getValue());
    }
}