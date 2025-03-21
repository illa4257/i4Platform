package illa4257.i4Framework.base.events;

@SuppressWarnings("unused")
public interface SingleEvent extends IEvent {
    default SingleEvent combine(final SingleEvent old) { return this; }
}