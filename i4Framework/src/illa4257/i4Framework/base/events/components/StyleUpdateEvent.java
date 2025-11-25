package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class StyleUpdateEvent extends Event implements SingleEvent {
    public StyleUpdateEvent(final Component component) { super(component); }
    public StyleUpdateEvent(final Component component, final boolean isSystem) { super(component, isSystem); }
}