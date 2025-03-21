package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeParentEvent extends Event implements SingleEvent {
    public ChangeParentEvent() {}
    public ChangeParentEvent(final boolean isSystem) { super(isSystem); }
}