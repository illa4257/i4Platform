package i4Framework.swing.components;

import i4Framework.base.EventListener;
import i4Framework.base.components.Component;
import i4Framework.base.components.Container;
import i4Framework.base.events.components.AddComponentEvent;
import i4Framework.base.events.components.RemoveComponentEvent;
import i4Framework.base.events.components.RepaintEvent;

public interface ISwingComponent {
    Component getComponent();
    void dispose();

    static SwingComponent getComponent(final java.awt.Container t, final Component c) {
        for (final java.awt.Component co : t.getComponents())
            if (co instanceof SwingComponent && ((SwingComponent) co).getComponent() == c)
                return (SwingComponent) co;
        return null;
    }

    static EventListener[] registerContainer(final java.awt.Container t, Container c) {
        return new EventListener[] {
            c.addEventListener(AddComponentEvent.class, e -> {
                t.add(new SwingComponent(e.child));
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