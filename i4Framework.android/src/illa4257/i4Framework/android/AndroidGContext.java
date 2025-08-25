package illa4257.i4Framework.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Utils.media.Color;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.math.Vector2D;

public class AndroidGContext implements Context {
    public final Paint paint = new Paint();
    public Canvas canvas;

    public AndroidGContext() {}

    @Override
    public Object getClipI() {
        return null;
    }

    private final char[] buff = new char[1];

    @Override
    public float charWidth(char ch) {
        buff[0] = ch;
        return paint.measureText(buff, 0, buff.length);
    }

    @Override
    public Vector2D bounds(final String string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length(), bounds);
        return new Vector2D(bounds.width(), bounds.height() + paint.getFontMetrics().descent);
    }

    @Override
    public Vector2D bounds(char[] string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length, bounds);
        return new Vector2D(bounds.width(), bounds.height() + paint.getFontMetrics().descent);
    }

    @Override public void setColor(final Color color) { paint.setColor(color.toARGB()); }
    @Override public float getStrokeWidth() { return paint.getStrokeWidth(); }
    @Override public void setStrokeWidth(final float newWidth) { paint.setStrokeWidth(newWidth); }

    @Override
    public void setClipI(Object clipArea) {

    }

    @Override
    public void setClip(final IPath path) {
        if (path instanceof AndroidPath)
            canvas.clipPath(((AndroidPath) path).path);
    }

    @Override public void translate(final float x, final float y) { canvas.translate(x, y); }
    @Override public void scale(final float x, final float y) { canvas.scale(x, y); }

    @Override
    public IPath newPath() {
        return new AndroidPath();
    }

    @Override
    public void draw(final IPath path) {
        canvas.drawPath(((AndroidPath) path).path, paint);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
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
    public void drawImage(Image image, float x, float y, float width, float height) {
        canvas.drawBitmap(((AndroidImage) image.imageMap.computeIfAbsent(AndroidImage.class, ignored -> AndroidImage.compute(image))).bitmap, null,
                new Rect(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height)),
                paint);
    }
}