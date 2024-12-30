package illa4257.i4Framework.base.events.input;

public enum MouseButton {
    UNKNOWN_BUTTON(-1),
    BUTTON0(0),
    BUTTON1(1),
    BUTTON2(2),
    BUTTON3(3),
    BUTTON4(4),
    BUTTON5(5),
    BUTTON6(6),
    BUTTON7(7);

    private final int code;

    MouseButton(int code) {
        this.code = code;
    }

    public static MouseButton fromCode(int code) {
        for (MouseButton button : values())
            if (button.code == code)
                return button;
        return UNKNOWN_BUTTON;
    }
}