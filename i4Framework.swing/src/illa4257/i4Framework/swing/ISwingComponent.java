package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;

public interface ISwingComponent {
    Component getComponent();
    void dispose();

    static SwingComponent getComponent(final java.awt.Container t, final Component c) {
        for (final java.awt.Component co : t.getComponents())
            if (co instanceof SwingComponent && ((SwingComponent) co).getComponent() == c)
                return (SwingComponent) co;
        return null;
    }

    @SuppressWarnings("rawtypes")
    static EventListener[] registerContainer(final java.awt.Container t, Container c) {
        return new EventListener[] {
            c.addEventListener(AddComponentEvent.class, e -> {
                if (getComponent(t, e.child) != null)
                    return;
                final SwingComponent co = new SwingComponent(e.child);
                t.add(co);
                co.repaint();
            }),
            c.addEventListener(RemoveComponentEvent.class, e -> {
                final SwingComponent co = getComponent(t, e.child);
                if (co == null)
                    return;
                t.remove(co);
                co.dispose();
            })
        };
    }
}