package illa4257.i4Framework.headless;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;

import java.io.File;
import java.util.function.Function;

public class HeadlessFramework extends Framework {
    public final File localAppDataDir, appDataDir, appDir, cacheDir, tmpDir;

    public HeadlessFramework(final Framework framework, final String virtual) {
        if (virtual == null || virtual.isEmpty()) {
            localAppDataDir = framework.getLocalAppDataDir();
            appDataDir = framework.getAppDataDir();
            appDir = framework.getAppDir();
            cacheDir = framework.getCacheDir();
            tmpDir = framework.getTmpDir();
        } else {
            File d = framework.getLocalAppDataDir();
            localAppDataDir = d != null ? new File(d, virtual) : null;
            d = framework.getAppDataDir();
            appDataDir = d != null ? new File(d, virtual) : null;
            d = framework.getAppDir();
            appDir = d != null ? new File(d, virtual) : null;
            d = framework.getCacheDir();
            cacheDir = d != null ? new File(d, virtual) : null;
            d = framework.getTmpDir();
            tmpDir = d != null ? new File(d, virtual) : null;
        }

        Framework.registerFramework(this);
    }

    @Override public void fireAllWindows(final Function<Window, Event> event) {}
    @Override public boolean isUIThread(final Component component) { return false; }
    @Override public void invokeLater(final Runnable runnable) {}
    @Override public FrameworkWindow newWindow(final Window window) { return null; }

    @Override public File getLocalAppDataDir() { return localAppDataDir; }
    @Override public File getAppDataDir() { return appDataDir; }
    @Override public File getAppDir() { return appDir; }
    @Override public File getCacheDir() { return cacheDir; }
    @Override public File getTmpDir() { return tmpDir; }
}
