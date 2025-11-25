package illa4257.i4Framework.base.events.touchscreen;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.IMoveableInputEvent;

public class TouchEvent extends Event implements IMoveableInputEvent {
    public TouchEvent(final Component component, final float globalX, final float globalY, final float x, final float y, final boolean isSystem,
                      final int pointerId) {
        super(component, isSystem, pointerId, globalX, globalY, x, y);
    }
}