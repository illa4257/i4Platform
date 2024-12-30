package illa4257.i4Framework.base.events.input;

import illa4257.i4Framework.base.events.Event;

public class MouseDownEvent extends Event {
    public final int x, y, localX, localY;
    public final MouseButton button;

    public MouseDownEvent(final int x, final int y, final MouseButton button) {
        this.localX = this.x = x;
        this.localY = this.y = y;
        this.button = button;
    }

    public MouseDownEvent(final int x, final int y, final int localX, final int localY, final MouseButton button) {
        this.x = x;
        this.y = y;
        this.localX = localX;
        this.localY = localY;
        this.button = button;
    }
}