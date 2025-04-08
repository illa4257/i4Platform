package illa4257.i4Framework.desktop;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Utils.Arch;
import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

public abstract class DesktopFramework extends Framework {
    public DesktopFramework() {
        try {
            if (Arch.REAL.IS_WINDOWS) {
                if (Arch.REAL.osVer.major >= 10)
                    new WinThemeDetector(this).start();
            } else if (Arch.REAL.IS_LINUX)
                new GnomeThemeDetector(this).start();
        } catch (final Exception ex) {
            i4Logger.INSTANCE.log(Level.WARN, "Failed to initialize theme listener.");
            i4Logger.INSTANCE.log(ex);
        }
    }

    @Override protected void onSystemThemeChange(final String theme) { super.onSystemThemeChange(theme); }
}