package illa4257.i4Framework.desktop;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Utils.Arch;

public abstract class DesktopFramework extends Framework {
    public DesktopFramework() {
        if (Arch.REAL.IS_WINDOWS && Arch.REAL.osVer.major >= 10)
            new WinThemeDetector(this).start();
    }

    @Override protected void onSystemThemeChange(final String theme) { super.onSystemThemeChange(theme); }
}