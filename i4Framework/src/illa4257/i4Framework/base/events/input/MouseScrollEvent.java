package illa4257.i4Framework.base.events.input;

import illa4257.i4Framework.base.events.Event;

public class MouseScrollEvent implements Event {
    public final int x, y, localX, localY, scroll;

    public MouseScrollEvent(final int x, final int y, final int unitsToScroll) {
        this.localX = this.x = x;
        this.localY = this.y = y;
        this.scroll = unitsToScroll;
    }

    public MouseScrollEvent(final int x, final int y, final int localX, final int localY, final int unitsToScroll) {
        this.x = x;
        this.y = y;
        this.localX = localX;
        this.localY = localY;
        this.scroll = unitsToScroll;
    }
}