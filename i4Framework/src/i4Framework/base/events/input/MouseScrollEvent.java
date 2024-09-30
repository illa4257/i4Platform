package i4Framework.base.events.input;

import i4Framework.base.events.Event;

public class MouseScrollEvent extends Event {
    public final int scroll;

    public MouseScrollEvent(final int unitsToScroll) {
        scroll = unitsToScroll;
    }
}