package illa4257.i4Framework.base.events.keyboard;

import illa4257.i4Framework.base.components.Component;

public class KeyDownEvent extends KeyEvent {
    public KeyDownEvent(final Component component, final int keyCode, final char keyChar) {
        super(component, keyCode, keyChar);
    }

    public KeyDownEvent(final Component component, final int keyCode, final char keyChar, final boolean isSystem) {
        super(component, keyCode, keyChar, isSystem);
    }
}