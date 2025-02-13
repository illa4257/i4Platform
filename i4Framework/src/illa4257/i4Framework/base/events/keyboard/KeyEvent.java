package illa4257.i4Framework.base.events.keyboard;

import illa4257.i4Framework.base.events.Event;

public class KeyEvent implements Event {
    public static final int
        BACKSPACE = '\b', DELETE = 127, TAB = '\t', ENTER = '\n',

        /* Modifiers */
        SHIFT = 16, CTRL = 17, ALT = 18, CAPS_LOCK = 20,

        /* Arrows */
        LEFT = 37, UP = 38, RIGHT = 39, DOWN = 40,

        /* F-keys */
        F1 = 112, F2 = 113, F3 = 114, F4 = 115, F5 = 116, F6 = 117, F7 = 118, F8 = 119, F9 = 120,
        F10 = 121, F11 = 122, F12 = 123,

        WINDOWS_KEY = 524, CONTEXT_MENU = 525,

        COPY = 65485, PASTE = 65487
    ;

    public static boolean isNotVisible(final int key) {
        return
            /* Modifiers */
                (key >= SHIFT && key <= ALT) || key == CAPS_LOCK ||

            /* Arrows */
                (key >= LEFT && key <= DOWN) ||

            /* F-keys */
                (key >= F1 && key <= F12) ||

            /* Operations */
                key == WINDOWS_KEY || key == CONTEXT_MENU ||
                key == COPY || key == PASTE
        ;
    }

    public final int keyCode;
    public final char keyChar;

    public KeyEvent(final int keyCode, final char keyChar) {
        this.keyCode = keyCode;
        this.keyChar = keyChar;
    }
}