package illa4257.i4Framework.base.events;

public interface EventListener<T extends IEvent> {
    void run(final T event);
}