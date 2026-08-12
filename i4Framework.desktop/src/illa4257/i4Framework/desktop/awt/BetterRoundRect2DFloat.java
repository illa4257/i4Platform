package illa4257.i4Framework.desktop.awt;

import java.awt.geom.*;
import java.io.Serializable;

public class BetterRoundRect2DFloat extends RectangularShape implements Serializable {
    private static final long serialVersionUID = 8340898869672529283L;

    public float
            x, y, width, height,
            topLeftArcWidth, topLeftArcHeight,
            topRightArcWidth, topRightArcHeight,
            bottomLeftArcWidth, bottomLeftArcHeight,
            bottomRightArcWidth, bottomRightArcHeight
    ;

    public BetterRoundRect2DFloat() {}
    public BetterRoundRect2DFloat(final float x, final float y, final float w, final float h,
                                  final float topLeftArcWidth, final float topLeftArcHeight,
                                  final float topRightArcWidth, final float topRightArcHeight,
                                  final float bottomRightArcWidth, final float bottomRightArcHeight,
                                  final float bottomLeftArcWidth, final float bottomLeftArcHeight) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        this.topLeftArcWidth = topLeftArcWidth;
        this.topLeftArcHeight = topLeftArcHeight;
        this.topRightArcWidth = topRightArcWidth;
        this.topRightArcHeight = topRightArcHeight;
        this.bottomRightArcWidth = bottomRightArcWidth;
        this.bottomRightArcHeight = bottomRightArcHeight;
        this.bottomLeftArcWidth = bottomLeftArcWidth;
        this.bottomLeftArcHeight = bottomLeftArcHeight;
    }

    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getWidth() { return width; }
    @Override public double getHeight() { return height; }
    public double getTopLeftArcWidth() { return topLeftArcWidth; }
    public double getTopLeftArcHeight() { return topLeftArcHeight; }
    public double getTopRightArcWidth() { return topRightArcWidth; }
    public double getTopRightArcHeight() { return topRightArcHeight; }
    public double getBottomLeftArcWidth() { return bottomLeftArcWidth; }
    public double getBottomLeftArcHeight() { return bottomLeftArcHeight; }
    public double getBottomRightArcWidth() { return bottomRightArcWidth; }
    public double getBottomRightArcHeight() { return bottomRightArcHeight; }
    @Override public boolean isEmpty() { return width <= 0 || height <= 0; }

    public void setRoundRect(final double x, final double y, final double w, final double h,
                             final double topLeftArcWidth, final double topLeftArcHeight,
                             final double topRightArcWidth, final double topRightArcHeight,
                             final double bottomRightArcWidth, final double bottomRightArcHeight,
                             final double bottomLeftArcWidth, final double bottomLeftArcHeight
    ) {
        this.x = (float) x;
        this.y = (float) y;
        this.width = (float) w;
        this.height = (float) h;
        this.topLeftArcWidth = (float) topLeftArcWidth;
        this.topLeftArcHeight = (float) topLeftArcHeight;
        this.topRightArcWidth = (float) topRightArcWidth;
        this.topRightArcHeight = (float) topRightArcHeight;
        this.bottomRightArcWidth = (float) bottomRightArcWidth;
        this.bottomRightArcHeight = (float) bottomRightArcHeight;
        this.bottomLeftArcWidth = (float) bottomLeftArcWidth;
        this.bottomLeftArcHeight = (float) bottomLeftArcHeight;
    }

    public void setRoundRect(final double x, final double y, final double w, final double h,
                             final double arcWidth, final double arcHeight
    ) {
        this.x = (float) x;
        this.y = (float) y;
        this.width = (float) w;
        this.height = (float) h;
        this.topLeftArcWidth = this.topRightArcWidth = this.bottomLeftArcWidth = this.bottomRightArcWidth = (float) arcWidth;
        this.topLeftArcHeight = this.topRightArcHeight = this.bottomLeftArcHeight = this.bottomRightArcHeight = (float) arcHeight;
    }

    /*public void setRoundRect(final RoundRectangle2D roundRectangle2D) {
        this.x = (float) roundRectangle2D.getX();
        this.y = (float) roundRectangle2D.getY();
        this.width = (float) roundRectangle2D.getWidth();
        this.height = (float) roundRectangle2D.getHeight();
        this.topLeftArcWidth = this.topRightArcWidth = this.bottomLeftArcWidth = this.bottomRightArcWidth =
                (float) roundRectangle2D.getArcWidth();
        this.topLeftArcHeight = this.topRightArcHeight = this.bottomLeftArcHeight = this.bottomRightArcHeight =
                (float) roundRectangle2D.getArcHeight();
    }*/

    @Override
    public void setFrame(final double x, final double y, final double w, final double h) {
        this.x = (float) x;
        this.y = (float) y;
        this.width = (float) w;
        this.height = (float) h;
    }

    @Override
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float(x, y, width, height);
    }

    @Override
    public boolean contains(final double x, final double y) {
        System.out.println("f");
        final float sx = this.x, sy = this.y,
                ex = sx + width, ey = height;
        if (x < sx || y < sy || x >= ex || y >= ey)
            return false;
        // TODO: Implement
        return true;
    }

    @Override
    public boolean intersects(final double x, final double y, final double w, final double h) {
        System.out.println("i");
        return true;
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        System.out.println("d");
        return true;
    }

    @Override
    public PathIterator getPathIterator(final AffineTransform at) {
        return new BetterRoundRectIterator(this, at);
    }

    @Override
    public String toString() {
        return "BetterRoundRect2DFloat(" + getX() + ", " + getY() + ", " + getWidth() + ", " + getHeight() +
                ", radius=[" +
                topLeftArcWidth + " / " + topLeftArcHeight + ", " +
                topRightArcWidth + " / " + topRightArcHeight + ", " +
                bottomRightArcWidth + " / " + bottomRightArcHeight + ", " +
                bottomLeftArcWidth + " / " + bottomLeftArcHeight + "]";
    }
}