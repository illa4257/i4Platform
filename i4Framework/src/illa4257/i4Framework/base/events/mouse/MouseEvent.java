package illa4257.i4Framework.base.events.mouse;

import illa4257.i4Framework.base.events.Event;

public class MouseEvent extends Event {
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

    public MouseEvent(final int x, final int y, final boolean isSystem) {
        super(isSystem);
        this.localX = this.x = x;
        this.localY = this.y = y;
    }

    public MouseEvent(final int x, final int y, final int localX, final int localY, final boolean isSystem) {
        super(isSystem);
        this.x = x;
        this.y = y;
        this.localX = localX;
        this.localY = localY;
    }
}