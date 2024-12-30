package illa4257.i4Framework.base;

import illa4257.i4Framework.base.events.Event;

public interface EventListener<T extends Event> {
    void run(final T event);
}