package illa4257.i4Framework.desktop.awt;

import illa4257.i4Framework.base.graphics.*;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Framework.desktop.DesktopFramework;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.math.Vector2;
import illa4257.i4Framework.base.graphics.Context;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;

public class AWTContext implements Context {
    public static final i4Logger L = new i4Logger("AWT");

    public final Graphics2D graphics;
    public final Shape clip;

    public AWTContext(final Graphics2D g) {
        graphics = g;
        clip = g.getClip();
    }

    @Override
    public Object cloneTransform() {
        return graphics.getTransform();
    }

    @Override
    public void setTransform(final Object transform) {
        graphics.setTransform((AffineTransform) transform);
    }

    @Override
    public void transform(final Object transform) {
        graphics.transform((AffineTransform) transform);
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
    public void setPaint(final Paint paint) {
        if (paint instanceof Color)
            graphics.setPaint(((Color) paint).toAwtColor());
        else {
            L.e("Unsupported paint type", paint.getClass());
            graphics.setColor(Color.TRANSPARENT.toAwtColor());
        }
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
        else if (shape instanceof PathRecorder) {
            final AWTPath p = new AWTPath();
            ((PathRecorder) shape).applyTo(p);
            return p.path;
        } else
            i4Logger.INSTANCE.e("Unknown shape class:", shape.getClass());
        return null;
    }

    @Override
    public void setClip(final Object path) {
        graphics.setClip(unify(path));
        if (clip != null)
            graphics.clip(clip);
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
    public Object newRoundShape(final float x, final float y, final float width, final float height,
                                final float topLeftArcWidth, final float topLeftArcHeight,
                                final float topRightArcWidth, final float topRightArcHeight,
                                final float bottomRightArcWidth, final float bottomRightArcHeight,
                                final float bottomLeftArcWidth, final float bottomLeftArcHeight) {
        return new BetterRoundRect2DFloat(
                x, y, width, height,
                topLeftArcWidth, topLeftArcHeight,
                topRightArcWidth, topRightArcHeight,
                bottomRightArcWidth, bottomRightArcHeight,
                bottomLeftArcWidth, bottomLeftArcHeight
        );
    }

    @Override
    public void draw(final Object path) {
        graphics.draw(unify(path));
    }

    @Override
    public void fill(final Object path) {
        graphics.fill(unify(path));
    }

    @Override public void translate(float x, float y) { graphics.translate(x, y); }
    @Override public void scale(float x, float y) { graphics.scale(x, y); }
    @Override public void rotate(final float deg) { graphics.rotate(deg); }
    @Override public void skew(final float x, final float y) { graphics.shear(x, y); }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2) {
        graphics.drawLine(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
    }

    @Override
    public void drawRect(final float x, final float y, final float w, final float h) {
        graphics.drawRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void fillRect(final float x, final float y, final float w, final float h) {
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
    public void drawSprite(final Sprite sprite, float x, float y) {
        if (sprite instanceof Image) {
            final Image img = (Image) sprite;
            graphics.drawImage(((BufImgRef) img.imageMap.computeIfAbsent(BufImgRef.class,
                            ignored -> BufImgRef.compute(img))).image,
                    Math.round(x), Math.round(y), null);
            return;
        }
        Context.super.drawSprite(sprite, x, y);
    }

    @Override
    public void drawSprite(final Sprite sprite, final float x, final float y, final float width, final float height) {
        if (sprite instanceof Image) {
            final Image img = Cache.scale((Image) sprite, width, height);
            graphics.drawImage(((BufImgRef) img.imageMap.computeIfAbsent(BufImgRef.class,
                            ignored -> BufImgRef.compute(img))).image,
                    Math.round(x), Math.round(y), null);
            return;
        }
        Context.super.drawSprite(sprite, x, y, width, height);
    }
}