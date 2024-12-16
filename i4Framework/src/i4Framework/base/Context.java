package i4Framework.base;

public interface Context {
    Vector2D bounds(final String string);
    Vector2D bounds(final char[] string);

    void setColor(final Color color);
    void drawRect(final float x, final float y, final float w, final float h);
    void drawString(final String str, final float x, final float y);
    void drawString(final char[] str, final float x, final float y);

    void drawImage(final Image image, final float x, final float y, final float width, final float height);
}