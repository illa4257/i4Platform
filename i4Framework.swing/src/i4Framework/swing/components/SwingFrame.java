package i4Framework.swing.components;

import i4Framework.base.EventListener;
import i4Framework.base.Framework;
import i4Framework.base.FrameworkWindow;
import i4Framework.base.components.Component;
import i4Framework.base.components.Window;
import i4Framework.base.events.components.ChangePointEvent;
import i4Framework.base.events.VisibleEvent;
import i4Framework.base.events.components.ChangeTextEvent;
import i4Framework.base.events.components.RepaintEvent;
import i4Framework.base.events.window.CenterWindowEvent;
import i4Framework.swing.SwingFramework;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SwingFrame extends JFrame implements ISwingComponent, FrameworkWindow {
    public final Window window;
    public final Container root;
    public EventListener[] l;

    public SwingFrame() { this(null); }
    public SwingFrame(final Window window) {
        this.window = window == null ? new Window() : window;
        (root = this.getContentPane()).setLayout(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                //SwingFramework.remove(SwingWindow.this);
                //if (SwingFrame.this.window.isVisible())
                //    SwingFrame.this.window.setVisible(false);
                setVisible(false);
            }
        });
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (!isVisible())
                    return;
                SwingFrame.this.window.setSize(root.getWidth(), root.getHeight());
            }
        });
        setVisible(this.window.isVisible());
        setTitle(this.window.getTitle());
        l = ISwingComponent.registerContainer(root, this.window);
        registerListeners();
        SwingFramework.add(this);
    }

    @Override public Component getComponent() { return window; }
    @Override public Framework getFramework() { return SwingFramework.INSTANCE; }
    @Override public Window getWindow() { return window; }

    @Override
    public void setVisible(final boolean b) {
        setSize(window.width.calcInt(), window.height.calcInt());
        if (isVisible() != b) {
            if (b)
                pack();
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
        window.addDirectEventListener(ChangePointEvent.class, e -> setSize(window.width.calcInt(), window.height.calcInt()));
        window.addEventListener(RepaintEvent.class, e -> repaint());
        window.addDirectEventListener(CenterWindowEvent.class, event -> {
            pack();
            setLocationRelativeTo(null);
        });
        window.addDirectEventListener(VisibleEvent.class, e -> {
            if (isVisible() != e.value)
                setVisible(e.value);
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        SwingFramework.remove(this);
        for (final EventListener li : l)
            window.removeEventListener(li);
    }
}
