package illa4257.i4Framework.base.events;

public class Event implements IEvent {
    public final boolean isSystem;
    public boolean isPrevented = false, isParentPrevented = true;

    public Event() { isSystem = false; }
    public Event(final boolean isSystem) { this.isSystem = isSystem; }

    @Override public boolean isPrevented() { return isPrevented; }
    @Override public boolean isParentPrevented() { return isParentPrevented || isPrevented; }

    public Event prevent(final boolean prevent) { isPrevented = prevent; return this; }
    public Event parentPrevent(final boolean prevent) { isParentPrevented = prevent; return this; }
}