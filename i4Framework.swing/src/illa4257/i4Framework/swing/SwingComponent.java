package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.events.components.*;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.keyboard.KeyDownEvent;
import illa4257.i4Framework.base.events.keyboard.KeyPressEvent;
import illa4257.i4Framework.base.events.keyboard.KeyUpEvent;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.styling.Cursor;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.mouse.*;
import illa4257.i4Framework.base.styling.StyleSetting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static java.awt.Cursor.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SwingComponent extends JComponent implements ISwingComponent {
    public final Component component;
    public final EventListener[] listeners;
    public EventListener[] directListeners = null;

    private JFrame getFrame() {
        java.awt.Container c = getParent();
        JFrame f = null;
        while (c != null) {
            if (c instanceof JFrame)
                f = (JFrame) c;
            c = c.getParent();
        }
        return f;
    }

    private int getGlobalX(final MouseEvent event) {
        final JFrame f = getFrame();
        if (f != null)
            return event.getXOnScreen() - f.getX() - f.getInsets().left;
        return event.getXOnScreen();
    }

    private int getGlobalY(final MouseEvent event) {
        final JFrame f = getFrame();
        if (f != null)
            return event.getYOnScreen() - f.getY() - f.getInsets().top;
        return event.getYOnScreen();
    }

    public SwingComponent(final Component component) {
        this.component = component;
        setFocusable(component.isFocusable());
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent event) {
                component.fire(new MouseDownEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY(),
                        MouseButton.fromCode(event.getButton() - 1)));
            }

            @Override
            public void mouseReleased(final MouseEvent event) {
                component.fire(new MouseUpEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY(),
                        MouseButton.fromCode(event.getButton() - 1)));
            }

            @Override
            public void mouseEntered(final MouseEvent event) {
                component.fire(new MouseEnterEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY()));
            }

            @Override
            public void mouseExited(final MouseEvent event) {
                component.fire(new MouseLeaveEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY()));
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent event) {
                component.fire(new MouseMoveEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY()));
            }

            @Override
            public void mouseDragged(final MouseEvent event) {
                component.fire(new MouseMoveEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY()));
            }
        });
        addMouseWheelListener(event -> {
            if (event.getUnitsToScroll() == 0)
                return;
            component.fire(new MouseScrollEvent(getGlobalX(event),
                    getGlobalY(event), event.getX(), event.getY(), event.getUnitsToScroll(), !event.isShiftDown() ?
                    Orientation.VERTICAL : Orientation.HORIZONTAL).parentPrevent(false));
        });
        addKeyListener(new KeyListener() {
            private final ArrayList<Integer> pressed = new ArrayList<>();

            @Override public void keyTyped(final KeyEvent e) {}

            @Override
            public void keyPressed(final KeyEvent e) {
                if (!pressed.contains(e.getKeyCode())) {
                    pressed.add(e.getKeyCode());
                    component.fire(new KeyDownEvent(e.getKeyCode(), e.getKeyChar()));
                }
                component.fire(new KeyPressEvent(e.getKeyCode(), e.getKeyChar()));
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                pressed.remove((Object) e.getKeyCode());
                component.fire(new KeyUpEvent(e.getKeyCode(), e.getKeyChar()));
            }
        });

        listeners = new EventListener[] {
                component.addEventListener(RecalculateEvent.class, e -> {
                    setLocation(component.startX.calcInt(), component.startY.calcInt());
                    setSize(component.width.calcInt(), component.height.calcInt());
                }),
                component.addEventListener(RepaintEvent.class, e -> repaint()),
                component.addEventListener(FocusEvent.class, e -> {
                    if (e.value)
                        requestFocus();
                }),
                component.addEventListener(ChangeZ.class, e -> {
                    final java.awt.Container p = getParent();
                    if (p == null)
                        return;
                    p.setComponentZOrder(this, e.z);
                }),
                component.addEventListener(VisibleEvent.class, e -> setVisible(e.value))
        };

        if (component instanceof Container) {
           directListeners = ISwingComponent.registerContainer(this, (Container) component);
           for (final Component co : (Container) component) {
               final SwingComponent c = new SwingComponent(co);
               add(c, 0);
               c.repaint();
           }
        }

        setVisible(component.isVisible());
        setLocation(component.startX.calcInt(), component.startY.calcInt());
        setSize(component.width.calcInt(), component.height.calcInt());

        component.subscribe("cursor", this::onCursorChange);

        StyleSetting s = component.getStyle("cursor");
        if (s != null)
            onCursorChange(s);
        else
            setCursor(getPredefinedCursor(DEFAULT_CURSOR));
    }

    private void onCursorChange(final StyleSetting s) {
        final Cursor c = s.cursor();
        final int cursor =
                c == Cursor.TEXT ? TEXT_CURSOR :
                c == Cursor.POINTER ? HAND_CURSOR :
                c == Cursor.N_RESIZE ? N_RESIZE_CURSOR :
                c == Cursor.SE_RESIZE ? SE_RESIZE_CURSOR :
                c == Cursor.E_RESIZE ? E_RESIZE_CURSOR :
                c == Cursor.EW_RESIZE ? E_RESIZE_CURSOR : // Not defined
                c == Cursor.NE_RESIZE ? NE_RESIZE_CURSOR :
                c == Cursor.NS_RESIZE ? N_RESIZE_CURSOR : // Not defined
                c == Cursor.NW_RESIZE ? NW_RESIZE_CURSOR :
                c == Cursor.NWSE_RESIZE ? MOVE_CURSOR :
                c == Cursor.S_RESIZE ? S_RESIZE_CURSOR :
                c == Cursor.SW_RESIZE ? SW_RESIZE_CURSOR :
                c == Cursor.W_RESIZE ? W_RESIZE_CURSOR :
                DEFAULT_CURSOR;
        setCursor(getPredefinedCursor(cursor));
    }

    @Override
    public void dispose() {
        if (listeners != null)
            for (final EventListener li : listeners)
                component.removeEventListener(li);
        if (directListeners != null)
            for (final EventListener li : directListeners)
                component.removeDirectEventListener(li);
    }

    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHints(SwingFramework.BEST);
        component.paint(new SwingContext(g));
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
    }

    @Override
    public Component getComponent() {
        return component;
    }

    @Override
    public String toString() {
        return component.toString() + super.toString();
    }
}
