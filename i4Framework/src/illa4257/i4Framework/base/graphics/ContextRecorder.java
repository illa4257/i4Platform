package illa4257.i4Framework.base.graphics;

import illa4257.i4Utils.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ContextRecorder implements Context, Sprite {
    public float width = 1, height = 1, strokeWidth = 1;
    public final List<Consumer<Context>> actions = new ArrayList<>();

    private static class Holder {
        public final ThreadLocal<Object> obj = new ThreadLocal<>();
    }

    public void applyTo(final Context context) {
        for (final Consumer<Context> action : actions)
            action.accept(context);
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public Object cloneTransform() {
        final Holder h = new Holder();
        actions.add(c -> h.obj.set(c.cloneTransform()));
        return h;
    }

    @Override
    public void setTransform(final Object transform) {
        actions.add(c -> c.setTransform(((Holder) transform).obj.get()));
    }

    @Override
    public void transform(final Object transform) {
        actions.add(c -> c.transform(((Holder) transform).obj.get()));
    }

    @Override
    public float charWidth(char ch) {
        return 0;
    }

    @Override
    public Vector2 bounds(String string) {
        return null;
    }

    @Override
    public Vector2 bounds(char[] string) {
        return null;
    }

    @Override
    public void setPaint(final Paint paint) {
        actions.add(c -> c.setPaint(paint));
    }

    @Override
    public float getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public void setStrokeWidth(final float newWidth) {
        actions.add(c -> c.setStrokeWidth(newWidth));
        strokeWidth = newWidth;
    }

    @Override
    public void setClip(final Object path) {
        actions.add(c -> c.setClip(path));
    }

    @Override
    public void translate(final float x, final float y) {
        actions.add(c -> c.translate(x, y));
    }

    @Override
    public void scale(final float x, final float y) {
        actions.add(c -> c.scale(x, y));
    }

    @Override
    public void rotate(final float deg) {
        actions.add(c -> c.rotate(deg));
    }

    @Override
    public void skew(final float x, final float y) {
        actions.add(c -> c.skew(x, y));
    }

    @Override
    public IPath newPath() {
        return new PathRecorder();
    }

    @Override
    public Object newRoundShape(float x, float y, float w, float h, float borderRadius) {
        final Holder r = new Holder();
        actions.add(c -> r.obj.set(c.newRoundShape(x, y, w, h, borderRadius)));
        return r;
    }

    @Override
    public Object newRoundShape(float x, float y, float w, float h, float topLeftArcWidth, float topLeftArcHeight, float topRightArcWidth, float topRightArcHeight, float bottomRightArcWidth, float bottomRightArcHeight, float bottomLeftArcWidth, float bottomLeftArcHeight) {
        final Holder r = new Holder();
        actions.add(c -> r.obj.set(c.newRoundShape(x, y, w, h,
                topLeftArcWidth, topLeftArcHeight, topRightArcWidth, topRightArcHeight,
                bottomRightArcWidth, bottomRightArcHeight, bottomLeftArcWidth, bottomLeftArcHeight)));
        return r;
    }

    @Override
    public void draw(final Object path) {
        actions.add(c -> c.draw(path));
    }

    @Override
    public void fill(final Object path) {
        actions.add(c -> c.fill(path));
    }

    @Override
    public void drawLine(final float x1, final float y1, final float x2, final float y2) {
        actions.add(c -> c.drawLine(x1, y1, x2, y2));
    }

    @Override
    public void drawRect(final float x, final float y, final float w, final float h) {
        actions.add(c -> c.drawRect(x, y, w, h));
    }

    @Override
    public void fillRect(final float x, final float y, final float w, final float h) {
        actions.add(c -> c.fillRect(x, y, w, h));
    }

    @Override
    public void drawString(final String str, final float x, final float y) {
        actions.add(c -> c.drawString(str, x, y));
    }

    @Override
    public void drawString(final char[] str, final float x, final float y) {
        actions.add(c -> c.drawString(str, x, y));
    }

    @Override
    public void drawSprite(final Sprite sprite, final float x, final float y) {
        actions.add(c -> c.drawSprite(sprite, x, y));
    }

    @Override
    public void drawSprite(final Sprite sprite, final float x, final float y, final float width, final float height) {
        actions.add(c -> c.drawSprite(sprite, x, y, width, height));
    }
}