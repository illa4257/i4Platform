package i4Utils.themeDetectors;

import i4Utils.OS;

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