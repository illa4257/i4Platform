package illa4257.i4Framework.base.events;

public interface SingleEvent extends Event {
    default SingleEvent combine(final SingleEvent old) { return this; }
}