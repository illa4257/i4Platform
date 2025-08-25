package illa4257.i4Framework.swing;

import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.desktop.BufImgRef;
import illa4257.i4Utils.media.Image;
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
    public Object getClipI() {
        return graphics.getClip();
    }

    @Override
    public float charWidth(final char ch) {
        return graphics.getFontMetrics().charWidth(ch);
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
    public float getStrokeWidth() {
        final Stroke s = graphics.getStroke();
        return s instanceof BasicStroke ? ((BasicStroke) s).getLineWidth() : 1;
    }

    @Override
    public void setStrokeWidth(final float newWidth) {
        graphics.setStroke(new BasicStroke(newWidth));
    }

    @Override
    public void setClipI(final Object clipArea) {
        graphics.setClip((Shape) clipArea);
    }

    @Override
    public void setClip(final IPath path) {
        graphics.setClip(((SwingPath) path).path);
    }

    @Override
    public SwingPath newPath() {
        return new SwingPath();
    }

    @Override
    public void draw(final IPath path) {
        graphics.draw(((SwingPath) path).path);
    }

    @Override public void translate(float x, float y) { graphics.translate(x, y); }
    @Override public void scale(float x, float y) { graphics.scale(x, y); }

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
        graphics.drawImage(((BufImgRef) image.imageMap.computeIfAbsent(BufImgRef.class,
                ignored -> BufImgRef.compute(image))).image,
                Math.round(x), Math.round(y), Math.round(width), Math.round(height), null);
    }
}