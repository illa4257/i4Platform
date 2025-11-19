package illa4257.i4Framework.base.events.keyboard;

public class KeyPressEvent extends KeyEvent {
    public KeyPressEvent(final int keyCode, final char keyChar) {
        super(keyCode, keyChar);
    }

    public KeyPressEvent(final int keyCode, final char keyChar, final boolean isSystem) {
        super(keyCode, keyChar, isSystem);
    }
}