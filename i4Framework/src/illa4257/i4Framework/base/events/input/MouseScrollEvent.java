package illa4257.i4Framework.base.events.input;

public class MouseScrollEvent extends MouseEvent {
    public final int scroll;

    public MouseScrollEvent(final int x, final int y, final int unitsToScroll) {
        super(x, y);
        this.scroll = unitsToScroll;
    }

    public MouseScrollEvent(final int x, final int y, final int localX, final int localY, final int unitsToScroll) {
        super(x, y, localX, localY);
        this.scroll = unitsToScroll;
    }
}