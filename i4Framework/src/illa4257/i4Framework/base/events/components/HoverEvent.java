package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.BaseEvent;
import illa4257.i4Framework.base.events.SingleEvent;

public class HoverEvent extends BaseEvent implements SingleEvent {
    public final boolean value;

    public HoverEvent(final boolean newValue) { value = newValue; }
}