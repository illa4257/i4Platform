package illa4257.i4Framework.base.events.window;

import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.SingleEvent;

public class CenterWindowEvent implements SingleEvent {
    public final Window window;

    public CenterWindowEvent(final Window window) { this.window = window; }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}
