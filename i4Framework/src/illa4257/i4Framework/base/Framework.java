package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.FileChooser;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.base.styling.StyleSelector;
import illa4257.i4Framework.base.styling.StyleSetting;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.res.ResourceManager;
import illa4257.i4Utils.res.ResourceProvider;
import illa4257.i4Utils.runnables.Consumer2;
import illa4257.i4Utils.web.i4URI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public abstract class Framework implements ResourceProvider {
    private static final boolean IS_DEV = "dev".equals(System.getProperty("env", "production"));
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
    protected volatile BaseTheme systemBaseTheme = BaseTheme.LIGHT, customBaseTheme = BaseTheme.LIGHT;

    private final Object systemThemeLocker = new Object();
    private final ConcurrentLinkedQueue<Consumer2<String, BaseTheme>> themeListeners = new ConcurrentLinkedQueue<>();

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

    public BaseTheme getBaseTheme() { return isSystemTheme ? systemBaseTheme : customBaseTheme; }

    protected void onSystemThemeChange(final String theme, final BaseTheme baseTheme) {
        if (theme == null)
            return;
        synchronized (systemThemeLocker) {
            if (theme.equals(systemTheme))
                return;
            systemTheme = theme;
            systemBaseTheme = baseTheme != null ? baseTheme : BaseTheme.LIGHT;
            if (isSystemTheme)
                try {
                    themeListeners.forEach(l -> l.accept(theme, baseTheme != null ? baseTheme : BaseTheme.LIGHT));
                } catch (final Throwable ex) {
                    i4Logger.INSTANCE.log(ex);
                }
        }
    }

    public void setTheme(final String theme, final BaseTheme baseTheme, final boolean followSystem) {
        isSystemTheme = followSystem;
        customTheme = theme;
        customBaseTheme = baseTheme;
        if (!isSystemTheme)
            try {
                themeListeners.forEach(l -> l.accept(theme, baseTheme));
            } catch (final Throwable ex) {
                i4Logger.INSTANCE.log(ex);
            }
    }

    public abstract void fireAllWindows(final Function<Window, Event> event);

    public boolean addThemeListener(final Consumer2<String, BaseTheme> listener) { return themeListeners.add(listener); }
    public boolean removeThemeListener(final Consumer2<String, BaseTheme> listener) { return themeListeners.remove(listener); }

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
        return uri.fullPath != null ? ClassLoader.getSystemResourceAsStream(uri.fullPath.startsWith("/") ?
                        uri.fullPath.substring(1) : uri.fullPath) : null;
    }

    public Image getImage(final InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException("Unsupported image format.");
    }

    public Image getImage(final i4URI uri) throws IOException { return getImage(openResource(uri)); }
    public Image getImage(final String uri) throws IOException { return getImage(openResource(uri)); }

    public File getAppDataDir() { return null; }
    public File getLocalAppDataDir() { return null; }
    public File getAppDir() { return null; }

    @Override
    public void addTo(final ResourceManager mgr) {
        mgr.add("assets", this);
        mgr.add("res", this);
    }

    public abstract FrameworkWindow newWindow(final Window window);
    public IFileChooser newFileChooser() { return new FileChooser(this); }
    public IFileChooser newFileChooser(final Window parent) {
        final IFileChooser fc = newFileChooser();
        fc.setParent(parent);
        return fc;
    }
    public ContextMenuBuilder newContextMenu() { return new ContextMenuBuilder(this); }

    public void dispose() {}

    public String getClipboardText() { return null; }
    public boolean setClipboardText(final CharSequence seq) { return false; }

    public boolean isDev() { return IS_DEV; }
}