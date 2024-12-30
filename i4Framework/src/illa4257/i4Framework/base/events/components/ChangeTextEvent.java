package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeTextEvent extends SingleEvent {
    public final String oldValue, newValue;

    public ChangeTextEvent(final String oldValue, final String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}