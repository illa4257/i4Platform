package illa4257.i4Framework.swing;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.math.Vector2D;

import java.awt.*;

public class SwingContext implements Context {
    public final Graphics2D graphics;

    public SwingContext(final Graphics2D g) {
        graphics = g;
        g.setRenderingHints(SwingFramework.current);
        g.setFont(SwingFramework.font);
    }


    @Override
    public Vector2D bounds(final String string) {
        return Vector2D.fromSize(graphics.getFontMetrics().getStringBounds(string, graphics));
    }

    @Override
    public Vector2D bounds(char[] string) {
        return Vector2D.fromSize(graphics.getFontMetrics().getStringBounds(string, 0, string.length, graphics));
    }

    @Override
    public void setColor(final Color color) {
        graphics.setColor((color != null ? color : Color.TRANSPARENT).toAwtColor());
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        graphics.drawLine(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
    }

    @Override
    public void drawRect(final float x, final float y, final float w, final float h) {
        graphics.fillRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void drawString(final String str, final float x, final float y) {
        final FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(str, (int) x, (int) y + metrics.getLeading() + metrics.getAscent());
    }

    @Override
    public void drawString(final char[] str, final float x, final float y) {
        final FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawChars(str, 0, str.length, (int) x, (int) y + metrics.getLeading() + metrics.getAscent());
    }

    @Override
    public void drawImage(Image image, float x, float y, float width, float height) {
        graphics.drawImage(image.asBufferedImage(), Math.round(x), Math.round(y), Math.round(width), Math.round(height), null);
    }
}