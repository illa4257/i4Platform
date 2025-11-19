package illa4257.i4Framework.base.events.keyboard;

public class KeyUpEvent extends KeyEvent {
    public KeyUpEvent(int keyCode, char keyChar) {
        super(keyCode, keyChar);
    }

    public KeyUpEvent(int keyCode, char keyChar, final boolean isSystem) {
        super(keyCode, keyChar, isSystem);
    }
}
