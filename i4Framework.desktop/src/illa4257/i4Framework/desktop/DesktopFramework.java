package illa4257.i4Framework.desktop;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.desktop.awt.AWTFileChooser;
import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.desktop.cheerpj.CheerpJThemeDetector;
import illa4257.i4Framework.desktop.win32.DwmAPI;
import illa4257.i4Framework.desktop.win32.WinFileChooser;
import illa4257.i4Framework.desktop.win32.WinThemeDetector;
import illa4257.i4Utils.Arch;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.runnables.Consumer2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class DesktopFramework extends Framework {
    public static Pointer getWindowPointer(final Window window) {
        if (window == null)
            return null;
        final FrameworkWindow fw = window.frameworkWindow.get();
        if (fw == null)
            return null;
        if (fw instanceof Frame)
            return Native.getWindowPointer((JFrame) fw);
        return null;
    }

    private final Object l = new Object();

    private final String appName;

    private volatile boolean startThemeDetector = true;

    public DesktopFramework(final String appName) {
        this.appName = appName;
        Framework.registerFramework(this);
    }

    protected static void setDarkMode(final Window window, final boolean enabled) {
        if (Arch.REAL.IS_WINDOWS && (
                    Arch.REAL.osVer.major > 11 ||
                    (Arch.REAL.osVer.major == 10 && Arch.REAL.osVer.build >= 1903) ||
                    (Arch.REAL.osVer.major == 11 && Arch.REAL.osVer.build >= 22000)
                )
        )
            try {
                final Pointer p = getWindowPointer(window);
                if (p == null)
                    return;
                DwmAPI.DwmSetWindowAttribute(new WinDef.HWND(p), DwmAPI.DWMWA_USE_IMMERSIVE_DARK_MODE,
                        new WinDef.BOOLByReference(new WinDef.BOOL(enabled)).getPointer(), 4);
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    @Override
    public FileChooser newFileChooser() {
        if (Arch.REAL.IS_WINDOWS)
            try {
                return new WinFileChooser();
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
        try {
            return new AWTFileChooser();
        } catch (final Throwable ex) {
            i4Logger.INSTANCE.log(ex);
        }
        return super.newFileChooser();
    }

    @Override
    public boolean addThemeListener(final Consumer2<String, BaseTheme> listener) {
        if (!super.addThemeListener(listener))
            return false;
        if (startThemeDetector) {
            final boolean s;
            synchronized (l) {
                if (s = startThemeDetector)
                    startThemeDetector = false;
            }
            if (s) {
                try {
                    startThemeDetector = false;
                    if (Arch.REAL.IS_WINDOWS) {
                        if (Arch.REAL.osVer.major >= 10)
                            new WinThemeDetector(this).start();
                    } else if (Arch.REAL.IS_LINUX)
                        (Arch.REAL.IS_CHEERPJ ? new CheerpJThemeDetector(this) :
                                new GnomeThemeDetector(this)).start();
                    else
                        i4Logger.INSTANCE.w("Unsupported environment.");
                } catch (final Throwable ex) {
                    i4Logger.INSTANCE.w("Failed to initialize theme listener.");
                    i4Logger.INSTANCE.w(ex);
                }
                onThemeDetectorInit();
            }
        }
        return true;
    }

    protected void onThemeDetectorInit() {}

    @Override public void onSystemThemeChange(final String theme, final BaseTheme baseTheme) { super.onSystemThemeChange(theme, baseTheme); }

    @Override
    public Image getImage(final InputStream inputStream) throws IOException {
        try {
            final BufferedImage i = ImageIO.read(inputStream);
            return new Image(i.getWidth(), i.getHeight(), BufImgRef.class, new BufImgRef(i));
        } catch (final Exception ignored) {
            return super.getImage(inputStream);
        }
    }

    @Override
    public File getAppDataDir() {
        return Arch.JVM.IS_WINDOWS ?
                        Arch.JVM.osVer.major >= 6 ? new File(System.getenv("APPDATA"), appName) :
                                new File(System.getProperty("user.home"), "Local Settings/Application Data/" + appName) :
                new File(System.getProperty("user.home"), Arch.JVM.IS_MACOS ? "/Library/Application Support/" + appName : "/.local/share/" + appName);
    }

    @Override
    public File getLocalAppDataDir() {
        return Arch.JVM.IS_WINDOWS ?
                Arch.JVM.osVer.major >= 6 ? new File(System.getenv("LOCALAPPDATA"), appName) :
                        new File(System.getProperty("user.home"), "Local Settings/Application Data/" + appName) :
                new File(System.getProperty("user.home"), Arch.JVM.IS_MACOS ? "/Library/Application Support/" + appName : "/.local/share/" + appName);
    }

    @Override
    public File getAppDir() { return MiniUtil.getPath(DesktopFramework.class); }

    @Override
    public String getClipboardText() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (final Exception ex) {
            i4Logger.INSTANCE.e(ex);
            return null;
        }
    }

    @Override
    public boolean setClipboardText(final CharSequence seq) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(seq instanceof String ? (String) seq : String.valueOf(seq)), null);
            return true;
        } catch (final Exception ex) {
            i4Logger.INSTANCE.e(ex);
            return false;
        }
    }

    public static Vector2 rectToV2(final Rectangle2D rect) {
        return new Vector2((float) rect.getWidth(), (float) rect.getHeight());
    }
}