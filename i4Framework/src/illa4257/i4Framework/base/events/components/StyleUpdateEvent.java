package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class StyleUpdateEvent extends Event implements SingleEvent {
    public StyleUpdateEvent() {}
    public StyleUpdateEvent(final boolean isSystem) { super(isSystem); }
}