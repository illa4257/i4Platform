package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.SingleEvent;

public class VisibleEvent extends SingleEvent {
    public final boolean value;

    public VisibleEvent(final boolean newValue) {
        value = newValue;
    }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}