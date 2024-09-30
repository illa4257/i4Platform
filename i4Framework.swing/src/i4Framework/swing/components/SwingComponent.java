package i4Framework.swing.components;

import i4Framework.base.EventListener;
import i4Framework.base.components.Button;
import i4Framework.base.components.Component;
import i4Framework.base.components.Container;
import i4Framework.base.events.components.ChangePointEvent;
import i4Framework.base.events.components.RecalculateEvent;
import i4Framework.base.events.components.RepaintEvent;
import i4Framework.base.events.input.ActionEvent;
import i4Framework.base.events.input.MouseScrollEvent;
import i4Framework.swing.SwingContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SwingComponent extends JComponent implements ISwingComponent {
    public final Component component;
    public final EventListener[] listeners;
    public EventListener[] directListeners = null;

    public SwingComponent(final Component component) {
        this.component = component;
        setFocusable(true);
        //setOpaque(false);

        if (component instanceof Button) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    component.fire(new ActionEvent());
                }
            });
        }
        addMouseWheelListener(event -> component.fire(new MouseScrollEvent(event.getUnitsToScroll())));

        listeners = new EventListener[] {
                component.addEventListener(RecalculateEvent.class, e -> {
                    setLocation(component.startX.calcInt(), component.startY.calcInt());
                    setSize(component.width.calcInt(), component.height.calcInt());
                }),
                component.addEventListener(RepaintEvent.class, e -> repaint())
        };

        if (component instanceof Container)
            directListeners = ISwingComponent.registerContainer(this, (Container) component);
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
        //final Graphics2D g = (Graphics2D) graphics.create(-component.startX.calcInt(), -component.startY.calcInt(), 0, 0);
        //g.setClip(null);
        final Graphics2D g = (Graphics2D) graphics;
        //g.setClip(null);
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
}
