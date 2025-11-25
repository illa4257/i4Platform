package illa4257.i4Framework.base.events.touchscreen;

import illa4257.i4Framework.base.components.Component;

public class TouchMoveEvent extends TouchEvent {
    public TouchMoveEvent(final Component component, final float globalX, final float globalY, final float x, final float y,
                          final boolean isSystem, final int pointerId) {
        super(component, globalX, globalY, x, y, isSystem, pointerId);
    }
}
