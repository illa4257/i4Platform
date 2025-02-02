package illa4257.i4Framework.base;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.math.Vector2D;

public interface Context {
    default Context sub(final float x, final float y, final float w, final float h) { return this; }
    default void dispose() {}

    Vector2D bounds(final String string);
    Vector2D bounds(final char[] string);

    void setColor(final Color color);
    void drawRect(final float x, final float y, final float w, final float h);
    void drawString(final String str, final float x, final float y);
    void drawString(final char[] str, final float x, final float y);

    void drawImage(final Image image, final float x, final float y, final float width, final float height);
}