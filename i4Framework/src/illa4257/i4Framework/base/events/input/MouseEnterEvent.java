package illa4257.i4Framework.base.events.input;

public class MouseEnterEvent extends MouseEvent {
    public MouseEnterEvent(final int x, final int y) {
        super(x, y);
    }

    public MouseEnterEvent(final int x, final int y, final int localX, final int localY) {
        super(x, y, localX, localY);
    }
}