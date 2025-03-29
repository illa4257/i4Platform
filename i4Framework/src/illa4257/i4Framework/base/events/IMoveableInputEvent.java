package illa4257.i4Framework.base.events;

public interface IMoveableInputEvent extends IEvent {
    default int id() { return 0; }
    default float x() { return 0; }
    default float y() { return 0; }
    default float globalX() { return 0; }
    default float globalY() { return 0; }
}