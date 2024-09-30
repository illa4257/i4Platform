package i4Framework.base.events.components;

import i4Framework.base.components.Component;
import i4Framework.base.events.SingleEvent;

public class ChangePointEvent extends SingleEvent {
    public final Component component;

    public ChangePointEvent(final Component component) { this.component = component; }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}