package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.math.Orientation;

public class MouseScrollEvent extends MouseEvent {
    public final Orientation orientation;
    public final int scroll;

    public MouseScrollEvent(final Component component, final int x, final int y, final int unitsToScroll, final Orientation orientation) {
        super(component, x, y);
        this.orientation = orientation;
        this.scroll = unitsToScroll;
    }

    public MouseScrollEvent(final Component component, final int x, final int y, final int localX, final int localY, final int unitsToScroll, final Orientation orientation) {
        super(component, x, y, localX, localY);
        this.orientation = orientation;
        this.scroll = unitsToScroll;
    }

    public MouseScrollEvent(final Component component, final int x, final int y, final int localX, final int localY, final boolean isSystem, final int unitsToScroll, final Orientation orientation) {
        super(component, x, y, localX, localY, isSystem);
        this.orientation = orientation;
        this.scroll = unitsToScroll;
    }
}