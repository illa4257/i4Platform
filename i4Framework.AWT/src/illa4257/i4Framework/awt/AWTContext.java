package illa4257.i4Framework.awt;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.desktop.BufImgRef;
import illa4257.i4Framework.desktop.DesktopFramework;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Utils.media.Color;
import illa4257.i4Utils.media.Image;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;

public class AWTContext implements Context {
    public final Graphics2D graphics;
    public final Shape clip;

    public AWTContext(final Graphics2D g) {
        graphics = g;
        clip = g.getClip();
        g.setRenderingHints(AWTFramework.current);
        g.setFont(AWTFramework.font);
    }

    @Override
    public Object font(final InputStream is, final float sz) {
        try {
            return Font.createFont(Font.PLAIN, is).deriveFont(sz);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void setFont(Object font) {
        graphics.setFont((Font) font);
    }

    @Override
    public void blur(boolean blur) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, blur ? RenderingHints.VALUE_INTERPOLATION_BILINEAR :
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    @Override
    public void antialiasing(boolean antialiasing) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public float charWidth(final char ch) {
        return graphics.getFontMetrics().charWidth(ch);
    }

    @Override
    public Vector2 bounds(final String string) {
        return DesktopFramework.rectToV2(graphics.getFontMetrics().getStringBounds(string, graphics));
    }

    @Override
    public Vector2 bounds(char[] string) {
        return DesktopFramework.rectToV2(graphics.getFontMetrics().getStringBounds(string, 0, string.length, graphics));
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

    private static Shape unify(final Object shape) {
        if (shape instanceof AWTPath)
            return ((AWTPath) shape).path;
        else if (shape instanceof Shape)
            return (Shape) shape;
        else
            i4Logger.INSTANCE.e("Unknown shape class:", shape.getClass());
        return null;
    }

    @Override
    public void setClip(final Object path) {
        final Shape shape = unify(path);
        if (clip != null)
            if (shape != null) {
                graphics.setClip(shape);
                graphics.clip(clip);
            } else
                graphics.setClip(clip);
    }

    @Override
    public AWTPath newPath() {
        return new AWTPath();
    }

    @Override
    public Object newRoundShape(final float x, final float y, final float w, final float h, final float borderRadius) {
        return new RoundRectangle2D.Float(x, y, w, h, borderRadius * 2, borderRadius * 2);
    }

    @Override
    public void draw(final Object path) {
        graphics.draw(unify(path));
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