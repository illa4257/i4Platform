package illa4257.i4Framework.base.events.touchscreen;

public class TouchDownEvent extends TouchEvent {
    public TouchDownEvent(final float globalX, final float globalY, final float x, final float y,
                          final boolean isSystem, final int pointerId) {
        super(globalX, globalY, x, y, isSystem, pointerId);
    }
}
