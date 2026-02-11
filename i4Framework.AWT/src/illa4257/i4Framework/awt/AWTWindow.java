package illa4257.i4Framework.awt;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.mouse.MouseButton;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;

import java.awt.*;
import java.awt.event.*;

public class AWTWindow extends Frame implements IAWTComponent, FrameworkWindow {
    public final Window window;
    public final AWTFramework framework;
    public final AWTComponent root;
    @SuppressWarnings("rawtypes")
    public EventListener[] l;
    private volatile boolean center = false;

    public AWTWindow(final AWTFramework framework) { this(framework, null); }
    public AWTWindow(final AWTFramework framework, final Window window) {
        if (framework == null)
            throw new IllegalArgumentException("Framework is null");
        this.framework = framework;
        this.window = window == null ? new Window() : window;
        root = new AWTComponent(this.window);
        setIgnoreRepaint(true);
        setLayout(null);
        add(root);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(final WindowEvent windowEvent) {
                if (!AWTWindow.this.window.frameworkWindow.setIfEquals(null, AWTWindow.this))
                    return;
                AWTWindow.this.window.unlink();
                framework.remove(AWTWindow.this);
                setVisible(false);
            }
        });
        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(final WindowEvent windowEvent) {
                if (!AWTWindow.this.window.isFocused())
                    AWTWindow.this.window.fire(new FocusEvent(AWTWindow.this.window, true, true));
            }

            @Override
            public void windowLostFocus(WindowEvent windowEvent) {
                if (AWTWindow.this.window.isFocused())
                    AWTWindow.this.window.fire(new FocusEvent(AWTWindow.this.window, false, true));
            }
        });
        setBackground(Color.BLACK);
        root.addComponentListener(new ComponentAdapter() {
            public void componentResized(final ComponentEvent e) {
                AWTWindow.this.window.setSize(root.getWidth(), root.getHeight(), true);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent mouseEvent) {
                AWTWindow.this.window.fire(new MouseDownEvent(AWTWindow.this.window, mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }

            @Override
            public void mouseReleased(final MouseEvent mouseEvent) {
                AWTWindow.this.window.fire(new MouseUpEvent(AWTWindow.this.window, mouseEvent.getX(), mouseEvent.getY(), MouseButton.fromCode(mouseEvent.getButton() - 1)));
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent componentEvent) {
                final Insets insets = getInsets();
                AWTWindow.this.window.setSize(getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom, true);
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
                window.fire(new StyleUpdateEvent(window));
                pack();
                framework.add(this);
                if (center) {
                    center = false;
                    setLocationRelativeTo(null);
                }
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
        final Insets insets = getInsets();
        super.setSize(i + insets.left + insets.right, i1 + insets.top + insets.bottom);
    }

    @Override
    public void update(Graphics graphics) {
        paint(graphics);
    }

    @SuppressWarnings("rawtypes")
    private EventListener[] registerListeners() {
        window.addDirectEventListener(ChangeTextEvent.class, e -> {
            if (e.component == AWTWindow.this.window)
                setTitle(String.valueOf(e.newValue));
        });
        window.addDirectEventListener(VisibleEvent.class, e -> {
            if (e.component == AWTWindow.this.window && isVisible() != e.value)
                setVisible(e.value);
        });

        return new EventListener[] {
                window.addEventListener(FocusEvent.class, e -> {
                    if (e.component == AWTWindow.this.window && e.value && !isFocused()) {
                        toFront();
                        requestFocus();
                    }
                }),
                window.addEventListener(RepaintEvent.class, e -> {
                    if (e.component == AWTWindow.this.window)
                        repaint();
                }),
                window.addDirectEventListener(CenterWindowEvent.class, e -> {
                    if (e.component != AWTWindow.this.window)
                        return;
                    if (isVisible())
                        setLocationRelativeTo(null);
                    else
                        center = true;
                })
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
