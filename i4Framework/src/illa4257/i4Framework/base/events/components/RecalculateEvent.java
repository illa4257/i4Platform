package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class RecalculateEvent extends Event implements SingleEvent {
    public RecalculateEvent(final Component component) { super(component); isParentPrevented = true; }
    public RecalculateEvent(final Component component, final boolean isSystem) { super(component, isSystem); isParentPrevented = true; }
}