package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class RecalculateEvent extends Event implements SingleEvent {
    public RecalculateEvent() {}
    public RecalculateEvent(final boolean isSystem) { super(isSystem); }
}