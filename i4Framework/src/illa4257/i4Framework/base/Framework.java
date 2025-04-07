package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Utils.logger.i4Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public abstract class Framework {
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

    protected final Object updateNotifier = new Object();
    protected boolean isUpdated;

    protected volatile boolean isSystemTheme = true;
    protected volatile String systemTheme = "light", customTheme = "light";

    private final ConcurrentLinkedQueue<Consumer<String>> themeListeners = new ConcurrentLinkedQueue<>();

    public boolean isSystemTheme() {
        return isSystemTheme;
    }

    /**
     * Expected values are typically {@code light} or {@code dark}.<br>
     * Format is {@code themeName}, but it can generally be any.
     *
     * @return The currently active theme.<br>
     * If {@link #isSystemTheme()} is true, returns the system-defined theme.<br>
     * Otherwise, returns the manually set custom theme.
     */
    public String getTheme() {
        return isSystemTheme ? systemTheme : customTheme;
    }

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

    public boolean addThemeListener(final Consumer<String> listener) {
        return themeListeners.add(listener);
    }

    public boolean removeThemeListener(final Consumer<String> listener) {
        return themeListeners.remove(listener);
    }

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

    public abstract FrameworkWindow newWindow(final Window window);
    public IFileChooser newFileChooser() { return new FileChooser(this); }

    public void dispose() {}
}