package illa4257.i4Framework.swing.components;

import illa4257.i4Framework.base.EventListener;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.input.MouseButton;
import illa4257.i4Framework.base.events.input.MouseDownEvent;
import illa4257.i4Framework.base.events.input.MouseUpEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.swing.SwingContext;
import illa4257.i4Framework.swing.SwingFramework;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SwingWindow extends JFrame implements ISwingComponent, FrameworkWindow {
    public final Window window;
    public final Container root;
    public EventListener[] l;

    public SwingWindow() { this(null); }
    public SwingWindow(final Window window) {
        this.window = window == null ? new Window() : window;
        setContentPane(root = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                SwingWindow.this.window.paint(new SwingContext((Graphics2D) graphics));
            }
        });
        root.setLayout(null);
        this.window.addEventListener(RepaintEvent.class, e -> root.repaint());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                setVisible(false);
            }
        });
        root.setBackground(Color.BLACK);
        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (!isVisible())
                    return;
                SwingWindow.this.window.endX.set(new PointAttach(root.getWidth(), null));
                SwingWindow.this.window.endY.set(new PointAttach(root.getHeight(), null));
                SwingWindow.this.window.fire(new ChangePointEvent(true));
                SwingWindow.this.window.repaint();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                SwingWindow.this.window.fire(new MouseDownEvent(mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                SwingWindow.this.window.fire(new MouseUpEvent(mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }
        });
        for (final Component co : this.window) {
            final SwingComponent c = new SwingComponent(co);
            add(c);
            c.repaint();
        }
        setVisible(this.window.isVisible());
        setTitle(this.window.getTitle());
        l = ISwingComponent.registerContainer(root, this.window);
        registerListeners();
    }

    @Override public Component getComponent() { return window; }
    @Override public Framework getFramework() { return SwingFramework.INSTANCE; }
    @Override public Window getWindow() { return window; }

    @Override
    public void pack() {
        setSize(window.width.calcInt(), window.height.calcInt());
        super.pack();
    }

    @Override
    public void setVisible(final boolean b) {
        if (isVisible() != b) {
            if (b) {
                if (!window.frameworkWindow.setIfNull(this))
                    return;
                window.link();
                pack();
                SwingFramework.add(this);
            } else {
                window.frameworkWindow.setIfEquals(null, this);
                window.unlink();
                SwingFramework.remove(this);
            }
            super.setVisible(b);
        }
        if (window.isVisible() != b) {
            window.setVisible(b);
            window.invokeAll();
        }
    }

    @Override
    public void setSize(int i, int i1) {
        if (window.width.calcInt() != i || window.height.calcInt() != i1)
            window.setSize(i, i1);
        if (root.getWidth() == i && root.getHeight() == i1)
            return;
        root.setPreferredSize(new Dimension(i, i1));
        final Insets insets = getInsets();
        super.setSize(i + insets.left + insets.right, i1 + insets.top + insets.bottom);
        revalidate();
        repaint();
    }

    private void registerListeners() {
        window.addDirectEventListener(ChangeTextEvent.class, e -> setTitle(e.newValue));
        window.addEventListener(ChangePointEvent.class, e -> {
            if (e.isSystem)
                return;
            setSize(window.width.calcInt(), window.height.calcInt());
        });
        window.addEventListener(RepaintEvent.class, e -> repaint());
        window.addEventListener(CenterWindowEvent.class, event -> setLocationRelativeTo(null));
        window.addDirectEventListener(VisibleEvent.class, e -> {
            if (isVisible() != e.value)
                setVisible(e.value);
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        for (final EventListener li : l)
            window.removeEventListener(li);
    }
}
