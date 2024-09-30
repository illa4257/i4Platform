package i4Framework.base.events;

public abstract class SingleEvent extends Event {
    public abstract SingleEvent combine(final SingleEvent old);
}