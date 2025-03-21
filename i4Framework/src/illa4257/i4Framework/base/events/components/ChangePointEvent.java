package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangePointEvent extends Event implements SingleEvent {
    public ChangePointEvent() {}
    public ChangePointEvent(final boolean isSystem) { super(isSystem); }
}