package i4Framework.base.events.components;

import i4Framework.base.events.SingleEvent;

public class RepaintEvent extends SingleEvent {
    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}