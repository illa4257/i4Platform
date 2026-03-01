package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.Button;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Label;
import illa4257.i4Framework.base.components.impl.FileChooserFallback;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import illa4257.i4Framework.base.points.PPointAdd;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.PointSet;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
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
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static illa4257.i4Framework.base.math.Unit.DP;

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

    public void writeImage(final Image image, final String format, final OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException("Unsupported image format.");
    }

    public File getAppDataDir() { return null; }
    public File getLocalAppDataDir() { return null; }
    public File getAppDir() { return null; }

    @Override
    public void addTo(final ResourceManager mgr) {
        mgr.add("assets", this);
        mgr.add("res", this);
    }

    public abstract FrameworkWindow newWindow(final Window window);
    public PopupMenu newPopupMenu(final Component component) { return new ContextMenuBuilder(this); }
    public Dialog newDialog(final Window window) {
        return new Dialog() {
            final Window d = new Window();

            final Point dp8 = new NumberPointMultiplier(8, d.densityMultiplier);
            final FrameworkWindow fw = newWindow(d);

            private final PointSet msgY = new PointSet(d.safeStartY), contentY = new PointSet(msgY);
            private final Point controlSY = new PPointAdd(contentY, dp8);
            private boolean hasControls;
            private final PointSet b1e = new PointSet(new PPointSubtract(d.safeEndX, dp8));
            private Component component = null;
            private Button pos = null, neg = null;
            private Label msg = null;
            private Runnable onCancel = null, onPos = null, onNeg = null;

            @Override
            public Dialog setTitle(final String title) {
                d.setTitle(title);
                return this;
            }

            @Override
            public Dialog setMessage(final String message) {
                if (msg == null) {
                    msg = new Label(message);
                    msg.setStartX(new PPointAdd(d.safeStartX, dp8));
                    msg.setStartY(new PPointAdd(d.safeStartY, dp8));
                    msg.setEndX(new PPointSubtract(d.safeEndX, dp8));
                    msg.setHeight(24, DP);
                    msgY.set(msg.endY);
                    d.add(msg);
                    return this;
                }
                msg.text = message;
                return this;
            }

            @Override
            public Dialog setContent(final Component component) {
                if (this.component != null)
                    d.remove(this.component);
                component.setStartX(new PPointAdd(d.safeStartX, dp8));
                component.setStartY(new PPointAdd(msgY, dp8));
                component.setEndX(new PPointSubtract(d.safeEndX, dp8));
                contentY.set(component.endY);
                d.add(this.component = component);
                return this;
            }

            @Override
            public Dialog setPositiveButton(final String name, final Runnable action) {
                this.onPos = action;
                if (pos == null) {
                    pos = new Button(name);
                    pos.setEndX(new PPointSubtract(d.safeEndX, dp8));
                    pos.setEndY(new PPointSubtract(d.safeEndY, dp8));
                    pos.setWidth(64, DP);
                    pos.setHeight(32, DP);
                    hasControls = true;
                    b1e.set(pos.startX);
                    pos.addEventListener(ActionEvent.class, e -> {
                        window.redirectFocus = null;
                        d.setVisible(false);
                        fw.dispose();
                        final Runnable onPos = this.onPos;
                        if (onPos != null)
                            onPos.run();
                    });
                    d.add(pos);
                    return this;
                }
                pos.setText(name);
                return this;
            }

            @Override
            public Dialog setNegativeButton(final String name, final Runnable action) {
                this.onNeg = action;
                if (neg == null) {
                    neg = new Button(name);
                    neg.setEndX(new PPointSubtract(b1e, dp8));
                    neg.setEndY(new PPointSubtract(d.safeEndY, dp8));
                    neg.setWidth(64, DP);
                    neg.setHeight(32, DP);
                    hasControls = true;
                    neg.addEventListener(ActionEvent.class, e -> {
                        window.redirectFocus = null;
                        d.setVisible(false);
                        fw.dispose();
                        final Runnable onNeg = this.onNeg;
                        if (onNeg != null)
                            onNeg.run();
                    });
                    d.add(neg);
                    return this;
                }
                neg.setText(name);
                return this;
            }

            @Override
            public Dialog setOnCancelListener(final Runnable action) {
                onCancel = action;
                return this;
            }

            @Override
            public Dialog show() {
                d.setWidth(400, DP);
                d.setHeight(hasControls ? new PPointAdd(new NumberPointMultiplier(40, window.densityMultiplier), controlSY) : controlSY);
                d.addDirectEventListener(VisibleEvent.class, e -> {
                    if (e.value)
                        return;
                    window.redirectFocus = null;
                    fw.dispose();
                    final Runnable cancel = onCancel;
                    if (cancel != null)
                        cancel.run();
                });
                d.center();
                d.setVisible(true);
                window.redirectFocus = d;
                return this;
            }
        };
    }

    public FileChooser newFileChooser() { return new FileChooserFallback(this); }
    public FileChooser newFileChooser(final Window parent) {
        final FileChooser fc = newFileChooser();
        fc.setParent(parent);
        return fc;
    }
    public ContextMenuBuilder newContextMenu() { return new ContextMenuBuilder(this); }

    public void dispose() {}

    public String getClipboardText() { return null; }
    public boolean setClipboardText(final CharSequence seq) { return false; }

    public boolean isDev() { return IS_DEV; }
}