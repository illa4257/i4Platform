package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.BaseEvent;
import illa4257.i4Framework.base.events.SingleEvent;

public class FocusEvent extends BaseEvent implements SingleEvent {
    public final boolean value;

    public FocusEvent(final boolean newValue) { value = newValue; }
}