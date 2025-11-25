package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.components.Component;

public class MouseMoveEvent extends MouseEvent {
    public MouseMoveEvent(final Component component, final int x, final int y) { super(component, x, y); }
    public MouseMoveEvent(final Component component, final int x, final int y, final int localX, final int localY) { super(component, x, y, localX, localY); }
    public MouseMoveEvent(final Component component, final float globalX, final float globalY, final float x, final float y,
                          final boolean isSystem, final int pointerId) {
        super(component, globalX, globalY, x, y, isSystem, pointerId);
    }
}
