package illa4257.i4Framework.base.events;

public class BaseEvent implements Event {
    public final boolean isSystem;

    public BaseEvent() { isSystem = false; }
    public BaseEvent(final boolean isSystem) { this.isSystem = isSystem; }
}