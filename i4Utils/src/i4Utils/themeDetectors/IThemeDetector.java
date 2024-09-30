package i4Utils.themeDetectors;

import i4Utils.OS;

public interface IThemeDetector {
    boolean canListen();
    OS.Theme getCurrentTheme();
    void stop();
}