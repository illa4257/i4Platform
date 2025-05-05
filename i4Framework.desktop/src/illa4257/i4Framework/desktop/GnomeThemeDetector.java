package illa4257.i4Framework.desktop;

import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.util.Scanner;

public class GnomeThemeDetector extends Thread {
    public final DesktopFramework framework;
    private final Process p;

    private boolean lastValue;

    public GnomeThemeDetector(final DesktopFramework framework) throws IOException {
        this.framework = framework;
        setName("Theme Detector (Linux Gnome Desktop Environment)");
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);

        final Process sp = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true)
                .start();

        try (final Scanner ss = new Scanner(sp.getInputStream())) {
            final String theme = ss.nextLine();
            //noinspection AssignmentUsedAsCondition
            if (lastValue = theme.equalsIgnoreCase("'prefer-dark'"))
                framework.onSystemThemeChange("dark", BaseTheme.DARK);
        }

        p = new ProcessBuilder("gsettings", "monitor", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true)
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
    }

    @Override
    public void run() {
        try (final Scanner s = new Scanner(p.getInputStream())) {
            String line;
            while (s.hasNextLine()) {
                line = s.nextLine().trim().toLowerCase();
                if (!line.startsWith("color-scheme:"))
                    continue;
                line = line.substring(13).trim();
                final boolean isDark = line.equalsIgnoreCase("'prefer-dark'");
                if (isDark == lastValue)
                    continue;
                lastValue = isDark;
                framework.onSystemThemeChange(isDark ? "dark" : "light", isDark ? BaseTheme.DARK : BaseTheme.LIGHT);
            }
        } catch (final Exception ex) {
            i4Logger.INSTANCE.log(ex);
        }
    }
}
