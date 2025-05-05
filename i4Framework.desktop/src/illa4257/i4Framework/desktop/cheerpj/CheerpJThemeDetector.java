package illa4257.i4Framework.desktop.cheerpj;

import illa4257.i4Framework.desktop.DesktopFramework;

public class CheerpJThemeDetector extends Thread {
    static {
        System.loadLibrary("i4Framework.desktop.cheerpj");
    }

    public final DesktopFramework framework;

    public CheerpJThemeDetector(final DesktopFramework framework) {
        this.framework = framework;
        setName("Theme Detector (CheerpJ)");
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    public native void run();
}