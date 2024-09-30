package i4Framework.base;

import i4Framework.base.events.Event;

public interface EventListener<T extends Event> {
    void run(final T event);
}