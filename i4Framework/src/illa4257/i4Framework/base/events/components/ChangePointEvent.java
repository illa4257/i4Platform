package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangePointEvent extends Event implements SingleEvent {
    public ChangePointEvent(final Component component) { super(component); }
    public ChangePointEvent(final Component component, final boolean isSystem) { super(component, isSystem); }
}