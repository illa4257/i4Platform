package i4Framework.base.events.window;

import i4Framework.base.components.Window;
import i4Framework.base.events.SingleEvent;

public class CenterWindowEvent extends SingleEvent {
    public final Window window;

    public CenterWindowEvent(final Window window) { this.window = window; }

    @Override public SingleEvent combine(final SingleEvent old) { return this; }
}
