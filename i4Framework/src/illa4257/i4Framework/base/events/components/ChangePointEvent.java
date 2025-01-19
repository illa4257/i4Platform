package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.BaseEvent;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangePointEvent extends BaseEvent implements SingleEvent {
    public ChangePointEvent() {}
    public ChangePointEvent(final boolean isSystem) { super(isSystem); }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}