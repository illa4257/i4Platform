package illa4257.i4Framework.android;

import android.graphics.*;
import android.graphics.Paint;
import illa4257.i4Framework.base.graphics.*;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Utils.math.Vector2;

import static illa4257.i4Framework.android.AndroidFramework.L;

public class AndroidGContext implements Context {
    private final Matrix matrix = new Matrix();
    public final Paint paint = new Paint();
    public Canvas canvas;
    public PropIter propIter;

    public AndroidGContext() {}

    public static Path getPath(final Object path) {
        if (path instanceof AndroidPath)
            return ((AndroidPath) path).path;
        else if (path instanceof Path)
            return (Path) path;
        else if (path instanceof PathRecorder) {
            final AndroidPath p = new AndroidPath();
            ((PathRecorder) path).applyTo(p);
            return p.path;
        }
        return null;
    }

    private final char[] buff = new char[1];

    @Override
    public PropIter getPropIter() {
        return propIter;
    }

    @Override
    public void setPropIter(final PropIter propIter) {
        this.propIter = propIter;
    }

    @Override
    public int getSaveCount() {
        return canvas.getSaveCount();
    }

    @Override
    public int save() {
        return canvas.save();
    }

    @Override
    public void restore() {
        canvas.restore();
    }

    @Override
    public void restoreToCount(final int count) {
        canvas.restoreToCount(count);
    }

    @Override
    public Object cloneTransform() {
        return new Matrix(matrix);
    }

    @Override
    public void setTransform(final Object transform) {
        matrix.set((Matrix) transform);
        canvas.setMatrix(matrix);
    }

    @Override
    public void transform(final Object transform) {
        matrix.preConcat((Matrix) transform);
        canvas.setMatrix(matrix);
    }

    @Override
    public float charWidth(char ch) {
        buff[0] = ch;
        return paint.measureText(buff, 0, buff.length);
    }

    @Override
    public Vector2 bounds(final String string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length(), bounds);
        return new Vector2(bounds.width(), bounds.height() + paint.getFontMetrics().descent);
    }

    @Override
    public Vector2 bounds(char[] string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length, bounds);
        return new Vector2(bounds.width(), bounds.height() + paint.getFontMetrics().descent);
    }

    private illa4257.i4Framework.base.graphics.Paint curPaint = null;

    @Override
    public illa4257.i4Framework.base.graphics.Paint getPaint() {
        return curPaint;
    }

    @Override
    public void setPaint(final illa4257.i4Framework.base.graphics.Paint paint) {
        this.curPaint = paint;
        if (paint instanceof Color)
            this.paint.setColor(((Color) paint).toARGB());
        else {
            L.e("Unsupported paint type", paint.getClass());
            this.paint.setColor(Color.TRANSPARENT.toARGB());
        }
    }
    @Override public float getStrokeWidth() { return paint.getStrokeWidth(); }
    @Override public void setStrokeWidth(final float newWidth) { paint.setStrokeWidth(newWidth); }

    @Override
    public void setClip(final Object path) {
        final Path p = getPath(path);
        if (p == null)
            return;
        canvas.clipPath(p);
    }

    @Override
    public void translate(final float x, final float y) {
        matrix.postTranslate(x, y);
        canvas.translate(x, y);
    }

    @Override
    public void scale(final float x, final float y) {
        matrix.postScale(x, y);
        canvas.scale(x, y);
    }

    @Override
    public void rotate(final float deg) {
        canvas.rotate(deg);
    }

    @Override
    public void skew(final float x, final float y) {
        canvas.skew(x, y);
    }

    @Override
    public IPath newPath() {
        return new AndroidPath();
    }

    @Override
    public Object newRoundShape(final float x, final float y, final float w, final float h, final float borderRadius) {
        final Path p = new Path();
        p.addRoundRect(x - paint.getStrokeWidth() / 2, y - paint.getStrokeWidth() / 2, w, h, borderRadius, borderRadius, Path.Direction.CW);
        return p;
    }

    @Override
    public Object newRoundShape(float x, float y, float w, float h, float topLeftArcWidth, float topLeftArcHeight, float topRightArcWidth, float topRightArcHeight, float bottomLeftArcWidth, float bottomLeftArcHeight, float bottomRightArcWidth, float bottomRightArcHeight) {
        final Path p = new Path();
        p.addRoundRect(
                x - paint.getStrokeWidth() / 2, y - paint.getStrokeWidth() / 2, w, h,
                new float[] {
                        topLeftArcWidth, topLeftArcHeight,
                        topRightArcWidth, topRightArcHeight,
                        bottomRightArcWidth, bottomRightArcHeight,
                        bottomLeftArcWidth, bottomLeftArcHeight
                },
                Path.Direction.CW
        );
        return p;
    }

    @Override
    public void draw(final Object path) {
        final Path p = getPath(path);
        if (p == null)
            return;
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(p, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void fill(final Object path) {
        final Path p = getPath(path);
        if (p == null)
            return;
        canvas.drawPath(p, paint);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(x, y, x + w, y + h, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void fillRect(float x, float y, float w, float h) {
        canvas.drawRect(x, y, x + w, y + h, paint);
    }

    @Override
    public void drawString(String str, float x, float y) {
        canvas.drawText(str, x, y - paint.getFontMetrics().ascent, paint);
    }

    @Override
    public void drawString(char[] str, float x, float y) {
        canvas.drawText(str, 0, str.length, x, y - paint.getFontMetrics().ascent, paint);
    }

    @Override
    public void drawSprite(final Sprite sprite, float x, float y) {
        if (sprite instanceof Image) {
            final Image image = (Image) sprite;
            canvas.drawBitmap(((AndroidImage) image.imageMap.computeIfAbsent(AndroidImage.class, ignored -> AndroidImage.compute(image))).bitmap,
                    Math.round(x), Math.round(y),
                    paint);
            return;
        }
        Context.super.drawSprite(sprite, x, y);
    }

    @Override
    public void drawSprite(final Sprite sprite, float x, float y, float width, float height) {
        if (sprite instanceof Image) {
            final Image image = (Image) sprite;
            canvas.drawBitmap(((AndroidImage) image.imageMap.computeIfAbsent(AndroidImage.class, ignored -> AndroidImage.compute(image))).bitmap, null,
                    new Rect(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height)),
                    paint);
            return;
        }
        Context.super.drawSprite(sprite, x, y, width, height);
    }
}