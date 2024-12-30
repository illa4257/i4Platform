package illa4257.i4Utils.themeDetectors;

import illa4257.i4Utils.OS;

/**
 * I'll move {@link i4Utils.themeDetectors} to {@link i4Framework}.
 *
 * @deprecated
 */
public interface IThemeDetector {
    boolean canListen();
    OS.Theme getCurrentTheme();
    void stop();
}