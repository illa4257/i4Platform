package i4Framework.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import i4Framework.base.Color;
import i4Framework.base.Context;
import i4Framework.base.Image;
import i4Framework.base.Vector2D;

public class AndroidGContext implements Context {
    public final Paint paint = new Paint();
    public Canvas canvas;

    {
        paint.setTextSize(50);
    }

    @Override
    public Vector2D bounds(final String string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length(), bounds);
        return new Vector2D(bounds.width(), bounds.height());
    }

    @Override
    public Vector2D bounds(char[] string) {
        final Rect bounds = new Rect();
        paint.getTextBounds(string, 0, string.length, bounds);
        return new Vector2D(bounds.width(), bounds.height());
    }

    @Override
    public void setColor(Color color) {
        paint.setColor(color.toARGB());
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        canvas.drawRect(x, y, x + w, y + h, paint);
    }

    @Override
    public void drawString(String str, float x, float y) {
        final Rect bounds = new Rect();
        paint.getTextBounds(str, 0, str.length(), bounds);
        canvas.drawText(str, x, y + bounds.height(), paint);
    }

    @Override
    public void drawString(char[] str, float x, float y) {
        canvas.drawText(str, 0, str.length, x, y, paint);
    }

    @Override
    public void drawImage(Image image, float x, float y, float width, float height) {
        canvas.drawBitmap(AndroidUtils.toBitmap(image), null,
                new Rect(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height)),
                paint);
    }
}