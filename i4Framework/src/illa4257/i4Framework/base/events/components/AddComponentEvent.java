package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.Event;

public class AddComponentEvent extends Event {
    public final Component child;
    public final Container container;

    public AddComponentEvent(final Container container, final Component child) { super(container); this.container = container; this.child = child; }
    public AddComponentEvent(final Container container, final Component child, final boolean isSystem) { super(container, isSystem); this.container = container; this.child = child; }
}