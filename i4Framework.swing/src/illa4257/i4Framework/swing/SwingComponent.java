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
    public EventListener[] listeners2 = null;
    private volatile int offset = 0;

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
                component.fire(new MouseDownEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(),
                        true, -1, MouseButton.fromCode(event.getButton() - 1)));
            }

            @Override
            public void mouseReleased(final MouseEvent event) {
                component.fire(new MouseUpEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(),
                        true, -1, MouseButton.fromCode(event.getButton() - 1)));
            }

            @Override
            public void mouseEntered(final MouseEvent event) {
                component.fire(new MouseEnterEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(), true));
            }

            @Override
            public void mouseExited(final MouseEvent event) {
                component.fire(new MouseLeaveEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(), true));
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(final MouseEvent event) {
                component.fire(new MouseMoveEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(), true, -1));
            }

            @Override
            public void mouseDragged(final MouseEvent event) {
                component.fire(new MouseMoveEvent(component, getGlobalX(event), getGlobalY(event), event.getX(), event.getY(), true, -1));
            }
        });
        addMouseWheelListener(event -> {
            if (event.getUnitsToScroll() == 0)
                return;
            component.fire(new MouseScrollEvent(component, getGlobalX(event),
                    getGlobalY(event), event.getX(), event.getY(), true, event.getUnitsToScroll(), !event.isShiftDown() ?
                    Orientation.VERTICAL : Orientation.HORIZONTAL).parentPrevent(false));
        });
        addKeyListener(new KeyListener() {
            private final ArrayList<Integer> pressed = new ArrayList<>();

            @Override public void keyTyped(final KeyEvent e) {}

            @Override
            public void keyPressed(final KeyEvent e) {
                if (!pressed.contains(e.getKeyCode())) {
                    pressed.add(e.getKeyCode());
                    component.fire(new KeyDownEvent(component, e.getKeyCode(), e.getKeyChar(), true));
                }
                component.fire(new KeyPressEvent(component, e.getKeyCode(), e.getKeyChar(), true));
            }

            @Override
            public void keyReleased(final KeyEvent e) {
                pressed.remove((Object) e.getKeyCode());
                component.fire(new KeyUpEvent(component, e.getKeyCode(), e.getKeyChar(), true));
            }
        });

        listeners = new EventListener[] {
                component.addEventListener(RecalculateEvent.class, e -> {
                    if (e.component == component)
                        updateLS(null);
                }),
                component.addEventListener(RepaintEvent.class, e -> repaint()),
                component.addEventListener(FocusEvent.class, e -> {
                    if (e.component == component && e.value)
                        requestFocus();
                }),
                component.addEventListener(ChangeZ.class, e -> {
                    if (e.component != component)
                        return;
                    final java.awt.Container p = getParent();
                    if (p == null)
                        return;
                    p.setComponentZOrder(this, e.z);
                }),
                component.addEventListener(VisibleEvent.class, e -> {
                    if (e.component == component)
                        setVisible(e.value);
                }),
                component.addEventListener(StyleUpdateEvent.class, e -> {
                    updateLS(null);
                })
        };

        if (component instanceof Container) {
           listeners2 = new EventListener[] {
                   component.addEventListener(AddComponentEvent.class, e -> {
                       if (e.container != component || ISwingComponent.getComponent(this, e.child) != null)
                           return;
                       final SwingComponent co = new SwingComponent(e.child);
                       add(co, 0);
                       co.repaint();
                   }),
                   component.addEventListener(RemoveComponentEvent.class, e -> {
                       if (e.container != component)
                           return;
                       final SwingComponent co = ISwingComponent.getComponent(this, e.child);
                       if (co == null)
                           return;
                       remove(co);
                       co.dispose();
                   })
           };
           for (final Component co : (Container) component) {
               final SwingComponent c = new SwingComponent(co);
               add(c, 0);
               c.repaint();
           }
        }

        component.subscribe("border-width", this::updateLS);
        component.subscribe("border-color", this::updateLS);
        component.subscribe("cursor", this::onCursorChange);

        setDropTarget(ISwingComponent.wrapDropTarget(component));
        setVisible(component.isVisible());
        updateLS(null);
        StyleSetting s = component.getStyle("cursor");
        if (s != null)
            onCursorChange(s);
        else
            setCursor(getPredefinedCursor(DEFAULT_CURSOR));
    }

    private int getBorderWidth() {
        return component.getColor("border-color").alpha > 0 ? Math.round(Math.max(component.calcStyleNumber("border-width", Orientation.HORIZONTAL, 0), 0)) : 0;
    }

    private void updateLS(final StyleSetting ignored) {
        final java.awt.Container p = getParent();
        final int bw = getBorderWidth(), o = p instanceof SwingComponent ? ((SwingComponent) p).getBorderWidth() : 0;
        setLocation(component.startX.calcInt() - bw + o, component.startY.calcInt() - bw + o);
        setSize(component.width.calcInt() + bw * 2, component.height.calcInt() + bw * 2);
        offset = bw;
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
        if (listeners2 != null)
            for (final EventListener li : listeners2)
                component.removeEventListener(li);
    }

    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHints(SwingFramework.BEST);
        g.translate(offset, offset);
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
