package illa4257.i4Framework.base.events.keyboard;

import illa4257.i4Framework.base.components.Component;

public class KeyPressEvent extends KeyEvent {
    public KeyPressEvent(final Component component, final int keyCode, final char keyChar) {
        super(component, keyCode, keyChar);
    }

    public KeyPressEvent(final Component component, final int keyCode, final char keyChar, final boolean isSystem) {
        super(component, keyCode, keyChar, isSystem);
    }
}