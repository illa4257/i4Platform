package illa4257.i4Framework.base.events.dnd;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.IMoveableInputEvent;
import illa4257.i4Framework.base.res.Res;

import java.util.List;

public class DropEvent extends Event implements IMoveableInputEvent {
    public final List<Res> resources;

    public DropEvent(final Component component, final boolean isSystem, final int pointerId, final float globalX, final float globalY,
                     final float x, final float y, final List<Res> resources) {
        super(component, isSystem, pointerId, globalX, globalY, x, y);
        this.resources = resources;
    }
}