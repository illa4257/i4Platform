package illa4257.i4Utils;

import illa4257.i4Utils.themeDetectors.DBusThemeDetector;
import illa4257.i4Utils.themeDetectors.IThemeDetector;

import java.util.ArrayList;
import java.util.function.Consumer;

public class OS {
    private static final ArrayList<Consumer<Theme>> themeList = new ArrayList<>();
    private static boolean isStarted = false;
    private static IThemeDetector themeDetector = null;

    public static final Arch ARCH = Arch.INSTANCE;

    /**
     * I'll move {@link illa4257.i4Utils.themeDetectors} to {@link illa4257.i4Framework}.
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
     * I'll move {@link illa4257.i4Utils.themeDetectors} to {@link illa4257.i4Framework}.
     *
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public static void fireThemeChanged(final Theme theme) {
        final Consumer<Theme>[] ll;
        synchronized (themeList) {
            ll = themeList.toArray(new Consumer[0]);
        }
        for (final Consumer<Theme> l : ll)
            l.accept(theme);
    }

    /**
     * I'll move {@link illa4257.i4Utils.themeDetectors} to {@link illa4257.i4Framework}.
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
     * I'll move {@link illa4257.i4Utils.themeDetectors} to {@link illa4257.i4Framework}.
     *
     * @deprecated
     */
    public static void addThemeListener(final Consumer<Theme> listener) {
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
     * I'll move {@link illa4257.i4Utils.themeDetectors} to {@link illa4257.i4Framework}.
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