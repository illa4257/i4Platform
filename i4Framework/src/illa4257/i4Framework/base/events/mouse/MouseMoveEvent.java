package illa4257.i4Framework.base.events.mouse;

public class MouseMoveEvent extends MouseEvent {
    public MouseMoveEvent(final int x, final int y) { super(x, y); }
    public MouseMoveEvent(final int x, final int y, final int localX, final int localY) { super(x, y, localX, localY); }
    public MouseMoveEvent(final float globalX, final float globalY, final float x, final float y,
                          final boolean isSystem, final int pointerId) {
        super(globalX, globalY, x, y, isSystem, pointerId);
    }
}
