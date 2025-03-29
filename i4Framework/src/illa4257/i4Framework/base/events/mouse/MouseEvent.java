package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.IMoveableInputEvent;

public class MouseEvent extends Event implements IMoveableInputEvent {
    public MouseEvent(final float x, final float y) {
        super(false, -1, x, y, x, y);
    }

    public MouseEvent(final float globalX, final float globalY, final float x, final float y) {
        super(false, -1, globalX, globalY, x, y);
    }

    public MouseEvent(final float x, final float y, final boolean isSystem) {
        super(isSystem, -1, x, y, x, y);
    }

    public MouseEvent(final float globalX, final float globalY, final float x, final float y, final boolean isSystem) {
        super(isSystem, -1, globalX, globalY, x, y);
    }

    public MouseEvent(final float globalX, final float globalY, final float x, final float y, final boolean isSystem,
                      final int pointerId) {
        super(isSystem, pointerId, globalX, globalY, x, y);
    }
}