package illa4257.i4Framework.awt;

import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.desktop.DesktopFramework;
import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.logger.i4Logger;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import static java.awt.RenderingHints.*;

public class AWTFramework extends DesktopFramework {
    public static final i4Logger L = new i4Logger("AWTFramework").registerHandler(i4Logger.INSTANCE);
    public static final Map<RenderingHints.Key, Object> BEST = Collections.unmodifiableMap(MiniUtil.put(new HashMap<>(),
            KEY_ANTIALIASING, VALUE_ANTIALIAS_ON,
            KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON,
            KEY_RENDERING, VALUE_RENDER_QUALITY,
            KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR,
            KEY_STROKE_CONTROL, VALUE_STROKE_PURE
    ));

    static {
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.uiScale.enabled", "false");
        System.setProperty("sun.java2d.dpiaware", "true");
    }

    Image buffer = null;

    public final ConcurrentLinkedQueue<AWTWindow> windows = new ConcurrentLinkedQueue<>();

    public volatile long DELAY = 1_000_000_000L / 60L;

    private volatile Thread loop = null;

    public AWTFramework(final String appName) { super(appName); }

    public void updateTheme(final Window window) {
        setDarkMode(window, getBaseTheme() == BaseTheme.DARK);
    }

    @Override
    protected void onThemeDetectorInit() {
        addThemeListener((theme, baseTheme) -> {
            final boolean isDark = baseTheme == BaseTheme.DARK;
            for (final AWTWindow w : windows)
                setDarkMode(w.window, isDark);
        });
    }

    public void check() {
        synchronized (windows) {
            if (windows.isEmpty()) {
                if (loop != null)
                    loop.interrupt();
                return;
            }
            if (loop != null)
                return;
            loop = new Thread(() -> {
                try {
                    long last = System.nanoTime();
                    while (!Thread.currentThread().isInterrupted()) {
                        boolean isNotRepeating = true;
                        for (final AWTWindow w : windows) {
                            w.window.invokeAll();
                            if (w.window.isRepeated())
                                isNotRepeating = false;
                            if (w.repaint) {
                                w.repaint = false;
                                EventQueue.invokeAndWait(() -> {
                                    final BufferStrategy s = w.getBufferStrategy();
                                    if (s == null) {
                                        w.paint(w.getGraphics());
                                        return;
                                    }
                                    do {
                                        do {
                                            final Graphics g = s.getDrawGraphics();
                                            if (g != null)
                                                try {
                                                    w.paint(g);
                                                } finally {
                                                    g.dispose();
                                                }
                                        } while (s.contentsRestored());
                                        s.show();
                                    } while (s.contentsLost());
                                    w.getToolkit().sync();
                                });
                            }
                        }

                        final long cur = System.nanoTime(), delta = Math.max(DELAY - cur + last, 0);
                        LockSupport.parkNanos(delta);
                        if (isNotRepeating)
                            try {
                                if (nextUpdate())
                                    last = System.nanoTime();
                                else
                                    last = cur + delta;
                            } catch (final InterruptedException ignored) {
                                break;
                            }
                    }
                } catch (final Throwable ex) {
                    L.e(ex);
                }
                synchronized (windows) {
                    if (loop == Thread.currentThread())
                        loop = null;
                }
            }, "AWTFramework Loop");
            loop.setPriority(Thread.MAX_PRIORITY);
            loop.start();
        }
    }

    @Override
    public void fireAllWindows(final Function<Window, Event> event) {
        for (final AWTWindow w : windows)
            w.window.fire(event.apply(w.window));
    }

    @Override
    public boolean isUIThread(final Component component) {
        return EventQueue.isDispatchThread();
    }

    @Override
    public void invokeLater(final Runnable runnable) {
        EventQueue.invokeLater(runnable);
    }

    @Override
    public FrameworkWindow newWindow(final Window window) {
        return new AWTWindow(this, window);
    }
}