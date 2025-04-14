package illa4257.i4Framework.desktop;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.IFileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.desktop.win32.WinFileChooser;
import illa4257.i4Utils.Arch;
import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import javax.swing.*;

public abstract class DesktopFramework extends Framework {
    public static Pointer getWindowPointer(final Window window) {
        if (window == null)
            return null;
        final FrameworkWindow fw = window.frameworkWindow.get();
        if (fw == null)
            return null;
        if (fw instanceof JFrame)
            return Native.getWindowPointer((JFrame) fw);
        return null;
    }

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

    @Override
    public IFileChooser newFileChooser() {
        if (Arch.REAL.IS_WINDOWS)
            try {
                return new WinFileChooser();
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
        return super.newFileChooser();
    }

    @Override protected void onSystemThemeChange(final String theme) { super.onSystemThemeChange(theme); }
}