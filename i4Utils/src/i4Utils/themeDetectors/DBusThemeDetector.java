package i4Utils.themeDetectors;

import i4Utils.OS;

import java.util.Scanner;

/**
 * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
 *
 * @deprecated
 */
public class DBusThemeDetector implements IThemeDetector {
    private final Process p;

    public DBusThemeDetector() {
        Process proc = null;
        try {
            final Process p = proc = new ProcessBuilder("dbus-monitor", "--session").start();
            final Thread t = new Thread(() -> {
                try (final Scanner s = new Scanner(p.getInputStream())) {
                    while (s.hasNextLine()) {
                        final String l = s.nextLine();
                        if (l.contains("/org/gnome/desktop/interface/color-scheme"))
                            OS.fireThemeChanged(getCurrentTheme());
                    }
                }
            });
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        } catch (final Exception ignored) {}
        p = proc;
    }

    @Override
    public OS.Theme getCurrentTheme() {
        try {
            final Process sp = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme").start();
            try (final Scanner ss = new Scanner(sp.getInputStream())) {
                final String theme = ss.nextLine();
                return theme.equals("'prefer-dark'") ? OS.Theme.DARK :
                        theme.equals("'prefer-light'") ? OS.Theme.LIGHT :
                                theme.equals("'default'") ? OS.Theme.DEFAULT : OS.Theme.UNKNOWN;
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            return OS.Theme.UNKNOWN;
        }
    }

    @Override
    public boolean canListen() {
        return p != null;
    }

    @Override
    public void stop() {
        if (p != null)
            p.destroy();
    }
}