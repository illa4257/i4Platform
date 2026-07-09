package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeParentEvent extends Event implements SingleEvent {
    public final Container oldValue, newValue;
    public ChangeParentEvent(final Component component, final Container oldValue, final Container newValue) { super(component); this.oldValue = oldValue; this.newValue = newValue; isParentPrevented = true; }
    public ChangeParentEvent(final Component component, final Container oldValue, final Container newValue, final boolean isSystem) { super(component, isSystem); this.oldValue = oldValue; this.newValue = newValue; isParentPrevented = true; }
}