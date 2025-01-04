package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;

public class AddComponentEvent implements Event {
    public final Component child;

    public AddComponentEvent(final Component child) {
        this.child = child;
    }
}