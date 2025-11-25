package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.IMoveableInputEvent;

public class MouseEvent extends Event implements IMoveableInputEvent {
    public MouseEvent(final Component component, final float x, final float y) {
        super(component, false, -1, x, y, x, y);
    }

    public MouseEvent(final Component component, final float globalX, final float globalY, final float x, final float y) {
        super(component, false, -1, globalX, globalY, x, y);
    }

    public MouseEvent(final Component component, final float x, final float y, final boolean isSystem) {
        super(component, isSystem, -1, x, y, x, y);
    }

    public MouseEvent(final Component component, final float globalX, final float globalY, final float x, final float y, final boolean isSystem) {
        super(component, isSystem, -1, globalX, globalY, x, y);
    }

    public MouseEvent(final Component component, final float globalX, final float globalY, final float x, final float y, final boolean isSystem,
                      final int pointerId) {
        super(component, isSystem, pointerId, globalX, globalY, x, y);
    }
}