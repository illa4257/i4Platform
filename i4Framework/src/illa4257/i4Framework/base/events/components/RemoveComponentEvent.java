package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;

public class RemoveComponentEvent extends Event {
    public final Component child;

    public RemoveComponentEvent(final Component child) { this.child = child; }
    public RemoveComponentEvent(final Component child, final boolean isSystem) { super(isSystem); this.child = child; }
}