package illa4257.i4Framework.awt;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.points.numbers.NumberPoint;
import illa4257.i4Utils.Arch;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AWTWindow extends Frame implements FrameworkWindow {
    protected volatile boolean repaint = false;
    public final AWTFramework framework;
    public final Window window;
    protected volatile Font font;

    public AWTWindow(final AWTFramework framework, final Window window) {
        this.framework = framework;
        final Window w = this.window = window != null ? window : new Window();

        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        setVisible(w.isVisible());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                setVisible(false);
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                final Insets insets = getInsets();
                repaint = true;
                w.setSize(getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom, true);
            }
        });

        w.addEventListener(RepaintEvent.class, e -> repaint = true);
        w.addDirectEventListener(VisibleEvent.class, e -> setVisible(e.value));
        w.addEventListener(ChangeTextEvent.class, e -> {
            setTitle(w.getTitle());
        });
        w.addEventListener(ChangePointEvent.class, e -> {
            if (e.isSystem)
                return;
            final Insets insets = getInsets();
            setSize(w.width.calcInt() + insets.left + insets.right, w.height.calcInt() + insets.top + insets.bottom);
        });
        setTitle(w.getTitle());
    }

    @Override
    public void paint(final Graphics g) {
        Image buffer = framework.buffer;
        if (buffer == null)
            framework.buffer = buffer = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());
        else if (buffer.getWidth(null) < getWidth() || buffer.getHeight(null) < getHeight()) {
            framework.buffer = buffer = getGraphicsConfiguration().createCompatibleImage(
                    Math.max(buffer.getWidth(null), getWidth()),
                    Math.max(buffer.getHeight(null), getHeight()));
            System.gc();
        }
        final Insets insets = getInsets();
        final AWTContext ctx = new AWTContext((Graphics2D) buffer.getGraphics());
        ctx.translate(insets.left, insets.top);
        ctx.graphics.setRenderingHints(AWTFramework.BEST);
        ctx.graphics.setFont(font);
        window.paint(ctx);
        window.paintComponents(ctx);
        g.drawImage(buffer, 0, 0, null);
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }

    @Override
    public void setVisible(final boolean b) {
        if (b == isVisible())
            return;
        if (b) {
            if (!window.frameworkWindow.setIfNull(this))
                return;
            window.dp.set(new NumberPoint(getToolkit().getScreenResolution() / 96f));
            window.sp.set(window.dp);
            final Insets insets = getInsets();
            setSize(window.width.calcInt() + insets.left + insets.right, window.height.calcInt() + insets.top + insets.bottom);
            if (Arch.REAL.IS_WINDOWS)
                setLocationRelativeTo(null);
        }
        super.setVisible(b);
        if (b) {
            framework.updateTheme(window);
            createBufferStrategy(1);
            window.link();
            font = new Font(Font.DIALOG, Font.PLAIN, Math.round(16 * window.dp.calcFloat()));
            framework.windows.offer(this);
            window.fire(new StyleUpdateEvent(window));
            framework.check();
        } else {
            framework.windows.remove(this);
            framework.check();
            window.unlink();
            window.frameworkWindow.set(null);
            dispose();
        }
    }

    @Override public Framework getFramework() { return framework; }
    @Override public Window getWindow() { return window; }
}
