package illa4257.i4Framework.base.events;

public interface EventListener<T extends Event> {
    void run(final T event);
}