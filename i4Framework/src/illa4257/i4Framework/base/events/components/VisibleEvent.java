package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class VisibleEvent extends Event implements SingleEvent {
    public final boolean value;

    public VisibleEvent(final boolean newValue) { value = newValue; }
}