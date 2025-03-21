package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class RepaintEvent extends Event implements SingleEvent {
    public RepaintEvent() {}
    public RepaintEvent(final boolean isSystem) { super(isSystem); }
}