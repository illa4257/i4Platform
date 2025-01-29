package illa4257.i4Framework.base.events.mouse;

public class MouseUpEvent extends MouseEvent {
    public final MouseButton button;

    public MouseUpEvent(final int x, final int y, final MouseButton button) {
        super(x, y);
        this.button = button;
    }

    public MouseUpEvent(final int x, final int y, final int localX, final int localY, final MouseButton button) {
        super(x, y, localX, localY);
        this.button = button;
    }
}