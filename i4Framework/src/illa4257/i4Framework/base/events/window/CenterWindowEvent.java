package illa4257.i4Framework.base.events.window;

import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.SingleEvent;

public class CenterWindowEvent extends Event implements SingleEvent {
    public final Window window;

    public CenterWindowEvent(final Window window) { this.window = window; }
    public CenterWindowEvent(final Window window, final boolean isSystem) { super(isSystem); this.window = window; }
}
