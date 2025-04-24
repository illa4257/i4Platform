package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.mouse.MouseButton;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SwingWindow extends JFrame implements ISwingComponent, FrameworkWindow {
    public final Window window;
    public final SwingFramework framework;
    public final Container root;
    @SuppressWarnings("rawtypes")
    public EventListener[] l;

    public SwingWindow(final SwingFramework framework) { this(framework, null); }
    public SwingWindow(final SwingFramework framework, final Window window) {
        if (framework == null)
            throw new IllegalArgumentException("Framework is null");
        this.framework = framework;
        this.window = window == null ? new Window() : window;
        setContentPane(root = new SwingComponent(this.window));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(final WindowEvent windowEvent) {
                if (!SwingWindow.this.window.frameworkWindow.setIfEquals(null, SwingWindow.this))
                    return;
                SwingWindow.this.window.unlink();
                framework.remove(SwingWindow.this);
                setVisible(false);
            }
        });
        setBackground(Color.BLACK);
        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(final ComponentEvent e) {
                SwingWindow.this.window.setSize(root.getWidth(), root.getHeight(), true);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                SwingWindow.this.window.fire(new MouseDownEvent(mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }

            @Override
            public void mouseReleased(final MouseEvent mouseEvent) {
                SwingWindow.this.window.fire(new MouseUpEvent(mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }
        });
        l = registerListeners();
        setVisible(this.window.isVisible());
        setTitle(this.window.getTitle());
    }

    @Override public Component getComponent() { return window; }
    @Override public Framework getFramework() { return framework; }
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
                framework.add(this);
            } else {
                if (!window.frameworkWindow.setIfEquals(null, this))
                    return;
                window.unlink();
                framework.remove(this);
            }
            super.setVisible(b);
        }
        if (window.isVisible() != b) {
            window.setVisible(b);
            framework.updated();
        }
    }

    @Override
    public void setSize(final int i, final int i1) {
        if (root.getWidth() == i && root.getHeight() == i1)
            return;
        root.setPreferredSize(new Dimension(i, i1));
        final Insets insets = getInsets();
        super.setSize(i + insets.left + insets.right, i1 + insets.top + insets.bottom);
    }

    @SuppressWarnings("rawtypes")
    private EventListener[] registerListeners() {
        window.addDirectEventListener(ChangeTextEvent.class, e -> setTitle(e.newValue));
        window.addDirectEventListener(VisibleEvent.class, e -> {
            if (isVisible() != e.value)
                setVisible(e.value);
        });
        window.addDirectEventListener(ChangePointEvent.class, e -> {
            if (e.isSystem)
                return;
            setSize(window.width.calcInt(), window.height.calcInt());
        });
        return new EventListener[] {
                window.addEventListener(RepaintEvent.class, e -> repaint()),
                window.addEventListener(CenterWindowEvent.class, event -> setLocationRelativeTo(null))
        };
    }

    @Override
    public void asContextMenu() {
        setUndecorated(true);
        setLocation(MouseInfo.getPointerInfo().getLocation());
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(final WindowEvent windowEvent) {
                dispose();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        for (@SuppressWarnings("rawtypes") final EventListener li : l)
            //noinspection unchecked
            window.removeEventListener(li);
    }
}
