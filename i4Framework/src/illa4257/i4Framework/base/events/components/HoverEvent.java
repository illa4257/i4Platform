package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class HoverEvent extends Event implements SingleEvent {
    public final boolean value;

    public HoverEvent(final Component component, final boolean newValue) { super(component); value = newValue; }
}