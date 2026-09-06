package illa4257.i4Framework.base.graphics;

import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Utils.math.Vector2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ContextRecorder implements Context, Sprite {
    public float width = 1, height = 1, strokeWidth = 1;
    public final List<Consumer<ApplyState>> actions = new ArrayList<>();

    private static class Holder {
        public final ThreadLocal<Object> obj = new ThreadLocal<>();
    }

    public static class ApplyState {
        public final Context context;
        public final int root;

        public ApplyState(final Context context) {
            this.context = context;
            root = context.getSaveCount();
        }
    }

    public void applyTo(final Context context) {
        final ApplyState state = new ApplyState(context);
        for (final Consumer<ApplyState> action : actions)
            action.accept(state);
    }

    @Override
    public PropIter getPropIter() {
        throw new RuntimeException("Unsupported action.");
    }

    @Override
    public void setPropIter(final PropIter pi) {
        throw new RuntimeException("Unsupported action.");
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    private int counter = 1;

    @Override
    public int getSaveCount() {
        return counter;
    }

    @Override
    public int save() {
        actions.add(c -> c.context.save());
        return counter++;
    }

    @Override
    public void restore() {
        if (counter == 1)
            throw new RuntimeException("Called restore more times than it was supposed to be");
        counter--;
        actions.add(c -> c.context.restore());
    }

    @Override
    public void restoreToCount(final int count) {
        if (count < 1)
            throw new RuntimeException("Count is lower than 1");
        if (count > counter)
            throw new RuntimeException("Count is higher than the save count");
        counter = count;
        actions.add(c -> c.context.restoreToCount(c.root + count));
    }

    @Override
    public Object cloneTransform() {
        final Holder h = new Holder();
        actions.add(c -> h.obj.set(c.context.cloneTransform()));
        return h;
    }

    @Override
    public void setTransform(final Object transform) {
        actions.add(c -> c.context.setTransform(((Holder) transform).obj.get()));
    }

    @Override
    public void transform(final Object transform) {
        actions.add(c -> c.context.transform(((Holder) transform).obj.get()));
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

    private Paint paint = null;

    @Override
    public Paint getPaint() {
        return paint;
    }

    @Override
    public void setPaint(final Paint paint) {
        this.paint = paint;
        actions.add(c -> c.context.setPaint(paint));
    }

    @Override
    public float getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    public void setStrokeWidth(final float newWidth) {
        actions.add(c -> c.context.setStrokeWidth(newWidth));
        strokeWidth = newWidth;
    }

    @Override
    public void setClip(final Object path) {
        actions.add(c -> c.context.setClip(path));
    }

    @Override
    public void translate(final float x, final float y) {
        actions.add(c -> c.context.translate(x, y));
    }

    @Override
    public void scale(final float x, final float y) {
        actions.add(c -> c.context.scale(x, y));
    }

    @Override
    public void rotate(final float deg) {
        actions.add(c -> c.context.rotate(deg));
    }

    @Override
    public void skew(final float x, final float y) {
        actions.add(c -> c.context.skew(x, y));
    }

    @Override
    public IPath newPath() {
        return new PathRecorder();
    }

    @Override
    public Object newRoundShape(float x, float y, float w, float h, float borderRadius) {
        final Holder r = new Holder();
        actions.add(c -> r.obj.set(c.context.newRoundShape(x, y, w, h, borderRadius)));
        return r;
    }

    @Override
    public Object newRoundShape(float x, float y, float w, float h, float topLeftArcWidth, float topLeftArcHeight, float topRightArcWidth, float topRightArcHeight, float bottomRightArcWidth, float bottomRightArcHeight, float bottomLeftArcWidth, float bottomLeftArcHeight) {
        final Holder r = new Holder();
        actions.add(c -> r.obj.set(c.context.newRoundShape(x, y, w, h,
                topLeftArcWidth, topLeftArcHeight, topRightArcWidth, topRightArcHeight,
                bottomRightArcWidth, bottomRightArcHeight, bottomLeftArcWidth, bottomLeftArcHeight)));
        return r;
    }

    @Override
    public void draw(final Object path) {
        actions.add(c -> c.context.draw(path));
    }

    @Override
    public void fill(final Object path) {
        actions.add(c -> c.context.fill(path));
    }

    @Override
    public void drawLine(final float x1, final float y1, final float x2, final float y2) {
        actions.add(c -> c.context.drawLine(x1, y1, x2, y2));
    }

    @Override
    public void drawRect(final float x, final float y, final float w, final float h) {
        actions.add(c -> c.context.drawRect(x, y, w, h));
    }

    @Override
    public void fillRect(final float x, final float y, final float w, final float h) {
        actions.add(c -> c.context.fillRect(x, y, w, h));
    }

    @Override
    public void drawString(final String str, final float x, final float y) {
        actions.add(c -> c.context.drawString(str, x, y));
    }

    @Override
    public void drawString(final char[] str, final float x, final float y) {
        actions.add(c -> c.context.drawString(str, x, y));
    }

    @Override
    public void drawSprite(final Sprite sprite, final float x, final float y) {
        actions.add(c -> c.context.drawSprite(sprite, x, y));
    }

    @Override
    public void drawSprite(final Sprite sprite, final float x, final float y, final float width, final float height) {
        actions.add(c -> c.context.drawSprite(sprite, x, y, width, height));
    }

    @Override
    public void with(final Consumer<Context> runnable) {
        actions.add(c -> c.context.with(runnable));
    }
}