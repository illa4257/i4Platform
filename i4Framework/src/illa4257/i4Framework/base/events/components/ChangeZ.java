package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class ChangeZ extends Event implements SingleEvent {
    public final int z;
    public ChangeZ(final Component component, final int z) { super(component); isParentPrevented = true; this.z = z; }
}