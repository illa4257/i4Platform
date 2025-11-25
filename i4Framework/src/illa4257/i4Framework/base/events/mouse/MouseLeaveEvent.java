package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.components.Component;

public class MouseLeaveEvent extends MouseEvent {
    public MouseLeaveEvent(final Component component, final int x, final int y) {
        super(component, x, y);
    }

    public MouseLeaveEvent(final Component component, final int x, final int y, final int localX, final int localY) {
        super(component, x, y, localX, localY);
    }

    public MouseLeaveEvent(final Component component, final int x, final int y, final int localX, final int localY, final boolean isSystem) {
        super(component, x, y, localX, localY, isSystem);
    }
}