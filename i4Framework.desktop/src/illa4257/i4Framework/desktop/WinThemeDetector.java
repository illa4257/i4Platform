package illa4257.i4Framework.desktop;

import com.sun.jna.platform.win32.*;

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
            framework.onSystemThemeChange("dark");
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

        while (!this.isInterrupted()) {
            err = Advapi32.INSTANCE.RegNotifyChangeKeyValue(hKey.getValue(), false, WinNT.REG_NOTIFY_CHANGE_LAST_SET, null, false);
            if (err != W32Errors.ERROR_SUCCESS)
                throw new Win32Exception(err);

            final boolean currentDetection = isDark();
            if (currentDetection != this.lastValue) {
                lastValue = currentDetection;
                framework.onSystemThemeChange(currentDetection ? "dark" : "light");
            }
        }
        Advapi32Util.registryCloseKey(hKey.getValue());
    }
}