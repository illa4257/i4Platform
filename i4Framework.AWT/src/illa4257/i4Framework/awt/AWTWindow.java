package illa4257.i4Framework.awt;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.points.numbers.NumberPoint;
import illa4257.i4Framework.base.styling.Cursor;
import illa4257.i4Framework.desktop.awt.AWTContext;
import illa4257.i4Framework.desktop.awt.AWTUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
        setExtendedState(Frame.NORMAL);
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

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                super.mouseMoved(e);
                final Insets insets = getInsets();
                final Component component = AWTWindow.this.window.find(getGlobalX(e), getGlobalY(e), new float[] { e.getX() - insets.left, e.getY() - insets.top });
                if (component == null)
                    return;
                setCursor(AWTUtils.getCursor(component.getPI().select("cursor", Cursor.class).nextLayer().nextSet()
                        .e(Cursor.class, Cursor.DEFAULT)));
            }
        });

        w.addEventListener(RepaintEvent.class, e -> repaint = true);
        w.addDirectEventListener(VisibleEvent.class, e -> setVisible(e.value));
        w.addEventListener(ChangeTextEvent.class, e -> setTitle(w.getTitle()));
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
        /*Image buffer = framework.buffer;
        if (buffer == null)
            framework.buffer = buffer = getGraphicsConfiguration().createCompatibleImage(getWidth(), getHeight());
        else if (buffer.getWidth(null) < getWidth() || buffer.getHeight(null) < getHeight()) {
            framework.buffer = buffer = getGraphicsConfiguration().createCompatibleImage(
                    Math.max(buffer.getWidth(null), getWidth()),
                    Math.max(buffer.getHeight(null), getHeight()));
            System.gc();
        }
        final Insets insets = getInsets();
        final AWTContext ctx = new AWTContext((Graphics2D) buffer.getGraphics());*/
        final Insets insets = getInsets();
        final AWTContext ctx = new AWTContext((Graphics2D) g);
        ctx.translate(insets.left, insets.top);
        ctx.graphics.setRenderingHints(AWTFramework.BEST);
        ctx.graphics.setFont(font);
        window.paint(ctx);
        window.paintComponents(ctx);
        //g.drawImage(buffer, 0, 0, null);
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
            setLocationByPlatform(true);
            final Insets insets = getInsets();
            setSize(window.width.calcInt() + insets.left + insets.right, window.height.calcInt() + insets.top + insets.bottom);
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

    private int getGlobalX(final MouseEvent event) {
        return event.getXOnScreen() - getX() - getInsets().left;
    }

    private int getGlobalY(final MouseEvent event) {
        return event.getYOnScreen() - getY() - getInsets().top;
    }
}
