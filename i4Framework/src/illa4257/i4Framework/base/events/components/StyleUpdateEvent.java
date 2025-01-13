package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.events.SingleEvent;

public class StyleUpdateEvent implements SingleEvent {
    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}
