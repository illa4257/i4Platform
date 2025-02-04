package illa4257.i4Framework.swing.components;

import illa4257.i4Framework.base.events.keyboard.KeyDownEvent;
import illa4257.i4Framework.base.events.keyboard.KeyTypeEvent;
import illa4257.i4Framework.base.events.keyboard.KeyUpEvent;
import illa4257.i4Framework.base.styling.Cursor;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;
import illa4257.i4Framework.base.events.mouse.*;
import illa4257.i4Framework.swing.SwingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.awt.Cursor.*;

public class SwingComponent extends JComponent implements ISwingComponent {
    public final Component component;
    public final EventListener[] listeners;
    public EventListener[] directListeners = null;

    private int getGlobalX(final MouseEvent event) {
        return getX() + event.getX();
    }

    private int getGlobalY(final MouseEvent event) {
        return getY() + event.getY();
    }

    public SwingComponent(final Component component) {
        this.component = component;
        setFocusable(true);
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

            @Override
            public void mouseMoved(final MouseEvent event) {
                component.fire(new MouseMoveEvent(getGlobalX(event), getGlobalY(event), event.getX(), event.getY()));
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
        addMouseWheelListener(event -> component.fire(new MouseScrollEvent(getGlobalX(event),
                getGlobalY(event), event.getX(), event.getY(), event.getUnitsToScroll())));
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                component.fire(new KeyTypeEvent(e.getKeyCode(), e.getKeyChar()));
            }

            @Override
            public void keyPressed(KeyEvent e) {
                component.fire(new KeyDownEvent(e.getKeyCode(), e.getKeyChar()));
            }

            @Override
            public void keyReleased(KeyEvent e) {
                component.fire(new KeyUpEvent(e.getKeyCode(), e.getKeyChar()));
            }
        });

        listeners = new EventListener[] {
                component.addEventListener(RecalculateEvent.class, e -> {
                    setLocation(component.startX.calcInt(), component.startY.calcInt());
                    setSize(component.width.calcInt(), component.height.calcInt());
                }),
                component.addEventListener(RepaintEvent.class, e -> repaint())
        };

        if (component instanceof Container) {
           directListeners = ISwingComponent.registerContainer(this, (Container) component);
           for (final Component co : (Container) component) {
               final SwingComponent c = new SwingComponent(co);
               add(c);
               c.repaint();
           }
        }

        setLocation(component.startX.calcInt(), component.startY.calcInt());
        setSize(component.width.calcInt(), component.height.calcInt());
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
        final Cursor c = component.getCursor("cursor");
        setCursor(getPredefinedCursor(c == Cursor.TEXT ? TEXT_CURSOR : DEFAULT_CURSOR));
        final Graphics2D g = (Graphics2D) graphics;
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
