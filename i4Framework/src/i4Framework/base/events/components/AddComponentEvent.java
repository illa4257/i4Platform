package i4Framework.base.events.components;

import i4Framework.base.components.Component;
import i4Framework.base.components.Container;
import i4Framework.base.events.Event;

public class AddComponentEvent extends Event {
    public final Container parent;
    public final Component child;

    public AddComponentEvent(final Container parent, final Component child) {
        this.parent = parent;
        this.child = child;
    }
}