package illa4257.i4Utils.lang;

public class LangString {
    public volatile String value;

    public LangString() { value = null; }
    public LangString(final String value) { this.value = value; }

    @Override public String toString() { return value; }
}
