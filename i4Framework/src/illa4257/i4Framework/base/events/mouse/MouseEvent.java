package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.events.Event;

public class MouseEvent implements Event {
    public final int x, y, localX, localY;

    public MouseEvent(final int x, final int y) {
        this.localX = this.x = x;
        this.localY = this.y = y;
    }

    public MouseEvent(final int x, final int y, final int localX, final int localY) {
        this.x = x;
        this.y = y;
        this.localX = localX;
        this.localY = localY;
    }
}