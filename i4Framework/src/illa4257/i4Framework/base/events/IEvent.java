package illa4257.i4Framework.base.events;

import illa4257.i4Framework.base.components.Component;

public interface IEvent {
    boolean isSystem();
    boolean isPrevented();
    default boolean isParentPrevented() { return true; }
    default Component getComponent() { return null; }
}