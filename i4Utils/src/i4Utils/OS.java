package i4Utils;

import i4Utils.themeDetectors.DBusThemeDetector;
import i4Utils.themeDetectors.IThemeDetector;

import java.util.ArrayList;

public class OS {
    private static final Object themeLocker = new Object();
    private static final ArrayList<Runnable1a<Theme>> themeList = new ArrayList<>();
    private static boolean isStarted = false;
    private static IThemeDetector themeDetector = null;

    public enum Theme {
        DEFAULT,
        LIGHT,
        DARK,
        UNKNOWN
    }

    @SuppressWarnings("unchecked")
    public static void fireThemeChanged(final Theme theme) {
        final Runnable1a<Theme>[] ll;
        synchronized (themeList) {
            ll = themeList.toArray(new Runnable1a[0]);
        }
        for (final Runnable1a<Theme> l : ll)
            l.run(theme);
    }

    public static Theme getCurrentTheme() {
        synchronized (themeList) {
            if (themeDetector == null)
                return Theme.UNKNOWN;
            return themeDetector.getCurrentTheme();
        }
    }

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