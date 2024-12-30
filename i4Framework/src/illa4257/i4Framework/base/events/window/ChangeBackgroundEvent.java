package illa4257.i4Framework.base.events.window;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeBackgroundEvent extends SingleEvent {
    public final Component component;
    public final Color newColor;

    public ChangeBackgroundEvent(final Component component, final Color newColor) {
        this.component = component;
        this.newColor = newColor;
    }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}
