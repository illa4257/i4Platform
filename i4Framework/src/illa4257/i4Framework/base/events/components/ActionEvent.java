package illa4257.i4Framework.base.events.components;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;

public class ActionEvent extends Event {
    public ActionEvent(final Component component) { super(component); }
    public ActionEvent(final Component component, final boolean isSystem) { super(component, isSystem); }
}