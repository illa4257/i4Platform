package illa4257.i4Framework.base.graphics;

import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.utils.Geom;
import illa4257.i4Utils.math.Vector2;

import java.io.InputStream;
import java.util.function.Consumer;

public interface Context {
    PropIter getPI();
    void setPI(final PropIter pi);

    Object cloneTransform();
    void setTransform(final Object transform);
    void transform(final Object transform);

    default Object font(final InputStream inputStream, final float sz) { return null; }
    default void blur(final boolean blur) {}
    default void antialiasing(final boolean antialiasing) {}
    default void setFont(final Object font) {}

    @SuppressWarnings("unused")
    Context sub(final float x, final float y, final float w, final float h);

    default Context apply(final Context config) {
        setPI(config.getPI());
        return this;
    }

    default void dispose() {}

    float charWidth(final char ch);
    Vector2 bounds(final String string);
    Vector2 bounds(final char[] string);

    void setPaint(final Paint paint);
    float getStrokeWidth();
    void setStrokeWidth(final float newWidth);
    void setClip(final Object path);
    void translate(final float x, final float y);
    void scale(final float x, final float y);
    void rotate(final float deg);
    void skew(final float x, final float y);


    IPath newPath();

    default Object newRoundShape(final float x, final float y, final float w, final float h, final float borderRadius) {
        final IPath p = newPath();
        final float ew = w - borderRadius, eh = h - borderRadius;

        p.moveTo(borderRadius + x, y);
        p.lineTo(ew + x, y); // Top Line
        p.arcTo(w + x, borderRadius + y, Geom.hPI, borderRadius);
        p.lineTo(w + x, eh + y); // Right line
        p.arcTo(ew + x, h + y, Geom.hPI, borderRadius);
        p.lineTo(borderRadius + x, h + y); // Bottom line
        p.arcTo(x, eh + y, Geom.hPI, borderRadius);
        p.lineTo(x, borderRadius + y); // Left line
        p.arcTo(borderRadius + x, y, Geom.hPI, borderRadius);

        p.close();
        return p;
    }

    Object newRoundShape(final float x, final float y, final float w, final float h,
                         final float topLeftArcWidth, final float topLeftArcHeight,
                         final float topRightArcWidth, final float topRightArcHeight,
                         final float bottomRightArcWidth, final float bottomRightArcHeight,
                         final float bottomLeftArcWidth, final float bottomLeftArcHeight);

    void draw(final Object path);
    void fill(final Object path);

    void drawLine(final float x1, final float y1, final float x2, final float y2);

    void drawRect(final float x, final float y, final float w, final float h);
    void fillRect(final float x, final float y, final float w, final float h);
    void drawString(final String str, final float x, final float y);
    void drawString(final char[] str, final float x, final float y);

    default void drawSprite(final Sprite sprite, final float x, final float y) {
        if (sprite instanceof ContextRecorder) {
            final ContextRecorder img = (ContextRecorder) sprite;
            final Context target = sub(x, y, img.getWidth(), img.getHeight());
            img.applyTo(target);
            return;
        }
        throw new RuntimeException(sprite.getClass().getName() + " is not supported");
    }

    default void drawSprite(final Sprite sprite, final float x, final float y, final float width, final float height) {
        if (sprite instanceof ContextRecorder) {
            final ContextRecorder img = (ContextRecorder) sprite;
            final Context target = sub(x, y, width, height);
            target.scale(width / img.getWidth(), height / img.getHeight());
            img.applyTo(target);
            return;
        }
        throw new RuntimeException(sprite.getClass().getName() + " is not supported");
    }

    default void with(final Consumer<Context> runnable) {
        runnable.accept(this);
    }
}