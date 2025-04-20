package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.styling.StyleSelector;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.res.ResourceManager;
import illa4257.i4Utils.res.ResourceProvider;
import illa4257.i4Utils.web.i4URI;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public abstract class Framework implements ResourceProvider {
    private static final ConcurrentLinkedQueue<Framework> frameworks = new ConcurrentLinkedQueue<>();

    public static void registerFramework(final Framework framework) {
        frameworks.add(framework);
    }

    public static boolean isThread(final Component component) {
        for (final Framework f : frameworks)
            if (f.isUIThread(component))
                return true;
        return false;
    }

    /// Global stylesheet
    public final ConcurrentLinkedQueue<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> stylesheet = new ConcurrentLinkedQueue<>();

    protected final Object updateNotifier = new Object();
    protected boolean isUpdated;

    protected volatile boolean isSystemTheme = true;
    protected volatile String systemTheme = "light", customTheme = "light";

    private final ConcurrentLinkedQueue<Consumer<String>> themeListeners = new ConcurrentLinkedQueue<>();

    public boolean isSystemTheme() { return isSystemTheme; }

    /**
     * Expected values are typically {@code light} or {@code dark}.<br>
     * Format is {@code themeName}, but it can generally be any.
     *
     * @return The currently active theme.<br>
     * If {@link #isSystemTheme()} is true, returns the system-defined theme.<br>
     * Otherwise, returns the manually set custom theme.
     */
    public String getTheme() { return isSystemTheme ? systemTheme : customTheme; }

    protected void onSystemThemeChange(final String theme) {
        systemTheme = theme;
        if (isSystemTheme)
            try {
                themeListeners.forEach(l -> l.accept(theme));
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    public void setTheme(final String theme, final boolean followSystem) {
        customTheme = theme;
        isSystemTheme = followSystem;
        if (!isSystemTheme)
            try {
                themeListeners.forEach(l -> l.accept(theme));
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    public abstract void fireAllWindows(final Event event);

    public boolean addThemeListener(final Consumer<String> listener) { return themeListeners.add(listener); }
    public boolean removeThemeListener(final Consumer<String> listener) { return themeListeners.remove(listener); }

    public void updated() {
        synchronized (updateNotifier) {
            if (isUpdated)
                return;
            isUpdated = true;
            updateNotifier.notifyAll();
        }
    }

    public abstract boolean isUIThread(final Component component);
    public abstract void invokeLater(final Runnable runnable);

    @Override
    public InputStream openResource(final i4URI uri) {
        return ClassLoader.getSystemResourceAsStream(uri.fullPath != null && uri.fullPath.startsWith("/") ?
                        uri.fullPath.substring(1) : uri.fullPath);
    }

    @Override
    public void addTo(final ResourceManager mgr) {
        mgr.add("assets", this);
        mgr.add("res", this);
    }

    public abstract FrameworkWindow newWindow(final Window window);
    public IFileChooser newFileChooser() { return new FileChooser(this); }
    public ContextMenuBuilder newContextMenu() { return new ContextMenuBuilder(this); }

    public void dispose() {}
}