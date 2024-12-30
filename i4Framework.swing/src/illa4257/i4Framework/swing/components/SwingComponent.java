package illa4257.i4Framework.swing.components;

import illa4257.i4Framework.base.EventListener;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.components.Panel;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;
import illa4257.i4Framework.base.events.input.MouseButton;
import illa4257.i4Framework.base.events.input.MouseDownEvent;
import illa4257.i4Framework.base.events.input.MouseScrollEvent;
import illa4257.i4Framework.base.events.input.MouseUpEvent;
import illa4257.i4Framework.swing.SwingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class SwingComponent extends JComponent implements ISwingComponent {
    public final Component component;
    public final EventListener[] listeners;
    public EventListener[] directListeners = null;

    public SwingComponent(final Component component) {
        this.component = component;
        setFocusable(true);
        setOpaque(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent event) {
                component.fire(new MouseDownEvent(getX() + event.getX(), getY() + event.getY(), event.getX(), event.getY(), MouseButton.fromCode(event.getButton() - 1)));
            }

            @Override
            public void mouseReleased(final MouseEvent event) {
                component.fire(new MouseUpEvent(getX() + event.getX(), getY() + event.getY(), event.getX(), event.getY(), MouseButton.fromCode(event.getButton() - 1)));
            }
        });

        addMouseWheelListener(event -> component.fire(new MouseScrollEvent(getX() + event.getX(), getY() + event.getY(), event.getX(), event.getY(), event.getUnitsToScroll())));

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
