package illa4257.i4Framework.base.events.keyboard;

import illa4257.i4Framework.base.events.Event;

public class KeyEvent implements Event {
    public static final int
        BACKSPACE = '\b', DELETE = 127, TAB = '\t', ENTER = '\n',

        SHIFT = 16,
        CTRL = 17,
        ALT = 18,

        LEFT = 37, UP = 38, RIGHT = 39, DOWN = 40,

        F1 = 112, F2 = 113, F3 = 114, F4 = 115, F5 = 116, F6 = 117, F7 = 118, F8 = 119, F9 = 120,
        F10 = 121, F11 = 122, F12 = 123,

        CONTEXT_MENU = 525,

        COPY = 65485, PASTE = 65487
    ;

    public final int keyCode;
    public final char keyChar;

    public KeyEvent(final int keyCode, final char keyChar) {
        this.keyCode = keyCode;
        this.keyChar = keyChar;
    }
}