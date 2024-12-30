package illa4257.i4Utils;

import illa4257.i4Utils.themeDetectors.DBusThemeDetector;
import illa4257.i4Utils.themeDetectors.IThemeDetector;

import java.util.ArrayList;

public class OS {
    private static final Object themeLocker = new Object();
    private static final ArrayList<Runnable1a<Theme>> themeList = new ArrayList<>();
    private static boolean isStarted = false;
    private static IThemeDetector themeDetector = null;

    /** CPU Architecture */
    public static final boolean
            IS_X86,
            IS_X86_64,

            IS_ARM,
            IS_ARM64;

    /** CPU bits */
    public static final boolean
            IS_32BIT,
            IS_64BIT;

    /** Operating Systems */
    public static final boolean
            IS_WINDOWS,

            IS_LINUX,
            IS_ANDROID,

            IS_MACOS;

    /** Platforms */
    public static final boolean
            IS_DESKTOP,
            IS_MOBILE;

    static {
        final String os = System.getProperty("os.name").toLowerCase(), arch = System.getProperty("os.arch").toLowerCase();

        // Architecture
        IS_X86_64 = arch.equals("amd64");
        IS_X86 = IS_X86_64 || arch.equals("x86");

        IS_ARM64 = arch.equals("aarch64");
        IS_ARM = IS_ARM64 || arch.equals("arm");

        // Bits
        IS_64BIT = IS_X86_64 || IS_ARM64;
        IS_32BIT = !IS_64BIT;

        // Operating System
        IS_WINDOWS = os.contains("win");
        if (IS_WINDOWS)
            IS_MACOS = IS_LINUX = false;
        else if (IS_LINUX = os.contains("nix") || os.contains("nux") || os.contains("aix"))
            IS_MACOS = false;
        else
            IS_MACOS = os.contains("mac");

        IS_ANDROID = IS_LINUX && System.getProperty("java.vendor").toLowerCase().contains("android");

        // Platforms
        IS_MOBILE = IS_ANDROID;
        IS_DESKTOP = !IS_MOBILE && (IS_WINDOWS || IS_LINUX || IS_MACOS);
    }

    /**
     * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
     *
     * @deprecated
     */
    public enum Theme {
        DEFAULT,
        LIGHT,
        DARK,
        UNKNOWN
    }

    /**
     * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
     *
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public static void fireThemeChanged(final Theme theme) {
        final Runnable1a<Theme>[] ll;
        synchronized (themeList) {
            ll = themeList.toArray(new Runnable1a[0]);
        }
        for (final Runnable1a<Theme> l : ll)
            l.run(theme);
    }

    /**
     * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
     *
     * @deprecated
     */
    public static Theme getCurrentTheme() {
        synchronized (themeList) {
            if (themeDetector == null)
                return Theme.UNKNOWN;
            return themeDetector.getCurrentTheme();
        }
    }

    /**
     * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
     *
     * @deprecated
     */
    public static void addThemeListener(final Runnable1a<Theme> listener) {
        if (listener == null)
            return;
        synchronized (themeList) {
            if (!themeList.add(listener))
                return;
            if (isStarted)
                return;
            isStarted = true;

            themeDetector = new DBusThemeDetector();
            if (!themeDetector.canListen())
                themeDetector = null;
        }
    }

    /**
     * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
     *
     * @deprecated
     */
    public static void removeAllThemeListeners() {
        synchronized (themeList) {
            if (themeList.isEmpty())
                return;
            themeList.clear();
            isStarted = false;
            if (themeDetector == null)
                return;
            themeDetector.stop();
            themeDetector = null;
        }
    }
}