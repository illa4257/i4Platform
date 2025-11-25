package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeTextEvent extends Event implements SingleEvent {
    public final Object oldValue, newValue;

    public ChangeTextEvent(final Component component, final Object oldValue, final Object newValue) {
        super(component);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public ChangeTextEvent(final Component component, final Object oldValue, final Object newValue, final boolean isSystem) {
        super(component, isSystem);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}