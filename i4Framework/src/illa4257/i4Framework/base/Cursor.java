package illa4257.i4Framework.base;

public enum Cursor {
    DEFAULT,
    TEXT;

    Cursor() {}

    public static Cursor fromName(final String name) {
        if (name == null)
            return DEFAULT;
        if (name.equalsIgnoreCase("text"))
            return TEXT;
        return DEFAULT;
    }

    public static Cursor from(final StyleSetting setting) {
        return fromName(setting.get(String.class));
    }
}