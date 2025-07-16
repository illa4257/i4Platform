package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Framework.desktop.DesktopFramework;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.logger.i4Logger;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static java.awt.RenderingHints.*;

public class SwingFramework extends DesktopFramework {
    static Font font;
    static Map<RenderingHints.Key, Object> current, RECOMMENDED;

    private final SyncVar<Instance> timer = new SyncVar<>();
    private class Instance {
        final ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor();
        boolean isNotRepeated;

        {
            s.submit(this::onTick);
        }

        public void onTick() {
            synchronized (updateNotifier) {
                if (isUpdated)
                    isUpdated = false;
            }
            final long last = System.currentTimeMillis();
            isNotRepeated = true;
            try {
                for (final SwingWindow w : frames)
                    try {
                        w.window.invokeAll();
                        if (isNotRepeated && w.window.isRepeated())
                            isNotRepeated = false;
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                Toolkit.getDefaultToolkit().sync();
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
            try {
                synchronized (updateNotifier) {
                    long current;
                    if (isUpdated || !isNotRepeated) {
                        current = System.currentTimeMillis();
                        final long d = 15 - current + last;
                        s.schedule(this::onTick, Math.max(d, 4), TimeUnit.MILLISECONDS);
                    } else {
                        updateNotifier.wait();
                        current = System.currentTimeMillis();
                        final long d = 15 - current + last;
                        if (d > 0)
                            s.schedule(this::onTick, d, TimeUnit.MILLISECONDS);
                        else
                            s.submit(this::onTick);
                    }
                }
            } catch (final Exception ex) {
                if (ex instanceof InterruptedException || ex instanceof RejectedExecutionException)
                    return;
                i4Logger.INSTANCE.log(ex);
            }
        }
    }

    static {
        final HashMap<RenderingHints.Key, Object> m = new HashMap<>();

        m.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        m.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        m.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        m.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        RECOMMENDED = Collections.unmodifiableMap(m);

        current = RECOMMENDED;

        font = new Font(Font.DIALOG, Font.PLAIN, 16);
    }

    private final ConcurrentLinkedQueue<SwingWindow> frames = new ConcurrentLinkedQueue<>();

    public SwingFramework(final String appName) {
        super(appName);
        addThemeListener((theme, baseTheme) -> {
            final boolean isDark = baseTheme == BaseTheme.DARK;
            for (final SwingWindow w : frames)
                setDarkMode(w.window, isDark);
        });
    }

    public void add(final SwingWindow frame) {
        if (frame == null)
            return;
        synchronized (frames) {
            final boolean isEmpty = frames.isEmpty();
            if (frames.add(frame) && isEmpty) {
                timer.computeIfAbsentP(Instance::new);
                setDarkMode(frame.window, getBaseTheme() == BaseTheme.DARK);
                frame.window.fire(new StyleUpdateEvent());
            }
        }
    }

    public void remove(final SwingWindow frame) {
        if (frame == null)
            return;
        synchronized (frames) {
            if (frames.remove(frame) && frames.isEmpty())
                stop();
        }
    }

    public void stop() {
        final Instance s = timer.getAndSet(null);
        if (s != null)
            s.s.shutdownNow();
    }

    @Override public boolean isUIThread(final Component component) { return SwingUtilities.isEventDispatchThread(); }
    @Override public void invokeLater(final Runnable runnable) { SwingUtilities.invokeLater(runnable); }

    @Override
    public void fireAllWindows(final Event event) {
        for (final SwingWindow w : frames)
            w.window.fire(event);
    }

    @Override public FrameworkWindow newWindow(final Window window) { return new SwingWindow(this, window); }

    @Override
    public void dispose() {
        super.dispose();
        stop();
    }
}