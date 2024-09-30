package i4Framework.swing;

import i4Framework.base.Framework;
import i4Framework.base.FrameworkWindow;
import i4Framework.base.components.Component;
import i4Framework.base.components.Window;
import i4Framework.swing.components.SwingFrame;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.awt.RenderingHints.*;

public class SwingFramework extends Framework {
    static Font font;
    static Map<RenderingHints.Key, Object> current, RECOMMENDED;
    public static final SwingFramework INSTANCE = new SwingFramework();

    private static final Object locker = new Object();
    private static Timer timer = null;

    static {
        Framework.registerFramework(INSTANCE);

        final HashMap<RenderingHints.Key, Object> m = new HashMap<>();

        m.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        m.put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
        m.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
        m.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);

        RECOMMENDED = Collections.unmodifiableMap(m);

        current = RECOMMENDED;

        font = new Font(Font.DIALOG, Font.PLAIN, 16);
    }

    private static final ConcurrentLinkedQueue<SwingFrame> frames = new ConcurrentLinkedQueue<>();
    public static void add(final SwingFrame frame) {
        if (frame == null)
            return;
        synchronized (frames) {
            if (frames.add(frame) && frames.size() == 1) {
                timer = new Timer(16, actionEvent -> {
                    for (final SwingFrame w : frames)
                        w.window.invokeAll();
                });
                timer.start();
            }
        }
    }

    public static void remove(final SwingFrame frame) {
        if (frame == null)
            return;
        synchronized (frames) {
            if (frames.remove(frame) && frames.isEmpty()) {
                timer.stop();
                timer = null;
            }
        }
    }

    @Override public boolean isUIThread(final Component component) { return SwingUtilities.isEventDispatchThread(); }
    @Override public FrameworkWindow newWindow(final Window window) { return new SwingFrame(window); }
}