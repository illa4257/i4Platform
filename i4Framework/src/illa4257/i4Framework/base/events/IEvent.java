package illa4257.i4Framework.base.events;

public interface IEvent {
    boolean isPrevented();
    default boolean isParentPrevented() { return true; }
}