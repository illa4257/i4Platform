package i4Framework.base.events;

public class VisibleEvent extends SingleEvent {
    public final boolean value;

    public VisibleEvent(final boolean newValue) {
        value = newValue;
    }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}