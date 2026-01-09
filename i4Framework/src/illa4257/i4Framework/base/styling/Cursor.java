package illa4257.i4Framework.base.styling;

public enum Cursor {
    DEFAULT,
    TEXT,
    POINTER,

    GRAB,
    GRABBING,

    E_RESIZE,
    EW_RESIZE,
    N_RESIZE,
    NE_RESIZE,
    NS_RESIZE,
    NW_RESIZE,
    NWSE_RESIZE,
    S_RESIZE,
    SE_RESIZE,
    SW_RESIZE,
    W_RESIZE;

    Cursor() {}

    public static Cursor fromName(final String name) {
        if (name == null)
            return DEFAULT;
        switch (name.replaceAll("_", "-").toLowerCase()) {
            case "text": return TEXT;
            case "pointer": return POINTER;
            case "grab": return GRAB;
            case "grabbing": return GRABBING;
            case "e-resize": return E_RESIZE;
            case "ew-resize": return EW_RESIZE;
            case "n-resize": return N_RESIZE;
            case "ne-resize": return NE_RESIZE;
            case "ns-resize": return NS_RESIZE;
            case "nw-resize": return NW_RESIZE;
            case "nwse-resize": return NWSE_RESIZE;
            case "s-resize": return S_RESIZE;
            case "se-resize": return SE_RESIZE;
            case "sw-resize": return SW_RESIZE;
            case "w-resize": return W_RESIZE;
        }
        return DEFAULT;
    }

    public static Cursor from(final StyleSetting setting) { return fromName(setting.get(String.class)); }
}