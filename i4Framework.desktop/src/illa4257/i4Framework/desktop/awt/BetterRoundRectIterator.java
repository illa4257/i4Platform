package illa4257.i4Framework.desktop.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;

public class BetterRoundRectIterator implements PathIterator {
    private static final double
            angle = Math.PI / 4,
            a = 1 - Math.cos(angle),
            b = Math.tan(angle),
            c = Math.sqrt(1 + b * b) - 1 + a,
            cv = 4.0 / 3 * a * b / c,
            acv = (1 - cv) / 2;

    private static final double[][] points = {
            { 0, 0, 0, .5 },
            { 0, 0, 1, -.5 },
            {
                    0, 0, 1, -acv,
                    0, acv, 1, 0,
                    0, .5, 1, 0
            },
            { 1, -.5, 1, 0 },
            {
                    1, -acv, 1, 0,
                    1, 0, 1, -acv,
                    1, 0, 1, -.5
            },
            { 1, 0, 0, .5 },
            {
                    1, 0, 0, acv,
                    1, -acv, 0, 0,
                    1, -.5, 0, 0
            },
            { 0, .5, 0, 0 },
            {
                    0, acv, 0, 0,
                    0, 0, 0, acv,
                    0, 0, 0, .5
            },
            {}
    };

    private final double x, y, w, h,
            topLeftArcWidth, topLeftArcHeight,
            topRightArcWidth, topRightArcHeight,
            bottomLeftArcWidth, bottomLeftArcHeight,
            bottomRightArcWidth, bottomRightArcHeight;
    private final AffineTransform transform;
    private int index;

    BetterRoundRectIterator(final BetterRoundRect2DFloat rect, final AffineTransform transform) {
        x = rect.getX();
        y = rect.getY();
        w = rect.getWidth();
        h = rect.getHeight();
        topLeftArcWidth      = Math.max(rect.getTopLeftArcWidth(), 0);
        topLeftArcHeight     = Math.max(rect.getTopLeftArcHeight(), 0);
        topRightArcWidth     = Math.max(rect.getTopRightArcWidth(), 0);
        topRightArcHeight    = Math.max(rect.getTopRightArcHeight(), 0);
        bottomLeftArcWidth   = Math.max(rect.getBottomLeftArcWidth(), 0);
        bottomLeftArcHeight  = Math.max(rect.getBottomLeftArcHeight(), 0);
        bottomRightArcWidth  = Math.max(rect.getBottomRightArcWidth(), 0);
        bottomRightArcHeight = Math.max(rect.getBottomRightArcHeight(), 0);
        this.transform = transform;
    }

    @Override
    public int getWindingRule() {
        return WIND_NON_ZERO;
    }

    @Override
    public boolean isDone() {
        return index >= points.length;
    }

    @Override
    public void next() {
        index++;
    }

    @Override
    public int currentSegment(final float[] coords) {
        if (isDone())
            throw new IndexOutOfBoundsException();
        final double[] s = points[index];
        final int type;
        final double arcWidth, arcHeight;
        switch (index) {
            case 0:
                type = SEG_MOVETO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            case 1:
                type = SEG_LINETO;
                arcWidth = bottomLeftArcWidth;
                arcHeight = bottomLeftArcHeight;
                break;
            case 2:
                type = SEG_CUBICTO;
                arcWidth = bottomLeftArcWidth;
                arcHeight = bottomLeftArcHeight;
                break;
            case 3:
                type = SEG_LINETO;
                arcWidth = bottomRightArcWidth;
                arcHeight = bottomRightArcHeight;
                break;
            case 4:
                type = SEG_CUBICTO;
                arcWidth = bottomRightArcWidth;
                arcHeight = bottomRightArcHeight;
                break;
            case 5:
                type = SEG_LINETO;
                arcWidth = topRightArcWidth;
                arcHeight = topRightArcHeight;
                break;
            case 6:
                type = SEG_CUBICTO;
                arcWidth = topRightArcWidth;
                arcHeight = topRightArcHeight;
                break;
            case 7:
                type = SEG_LINETO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            case 8:
                type = SEG_CUBICTO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            default:
                return SEG_CLOSE;
        }

        int c = 0;
        for (int i = 0; i < s.length; i += 4) {
            coords[c++] = (float) (x + s[i] * w + s[i + 1] * arcWidth);
            coords[c++] = (float) (y + s[i + 2] * h + s[i + 3] * arcHeight);
        }
        if (transform != null)
            transform.transform(coords, 0, coords, 0, c / 2);
        return type;
    }

    @Override
    public int currentSegment(double[] coords) {
        if (isDone())
            throw new IndexOutOfBoundsException();
        final double[] s = points[index];
        final int type;
        final double arcWidth, arcHeight;
        switch (index) {
            case 0:
                type = SEG_MOVETO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            case 1:
                type = SEG_LINETO;
                arcWidth = bottomLeftArcWidth;
                arcHeight = bottomLeftArcHeight;
                break;
            case 2:
                type = SEG_CUBICTO;
                arcWidth = bottomLeftArcWidth;
                arcHeight = bottomLeftArcHeight;
                break;
            case 3:
                type = SEG_LINETO;
                arcWidth = bottomRightArcWidth;
                arcHeight = bottomRightArcHeight;
                break;
            case 4:
                type = SEG_CUBICTO;
                arcWidth = bottomRightArcWidth;
                arcHeight = bottomRightArcHeight;
                break;
            case 5:
                type = SEG_LINETO;
                arcWidth = topRightArcWidth;
                arcHeight = topRightArcHeight;
                break;
            case 6:
                type = SEG_CUBICTO;
                arcWidth = topRightArcWidth;
                arcHeight = topRightArcHeight;
                break;
            case 7:
                type = SEG_LINETO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            case 8:
                type = SEG_CUBICTO;
                arcWidth = topLeftArcWidth;
                arcHeight = topLeftArcHeight;
                break;
            default:
                return SEG_CLOSE;
        }

        int c = 0;
        for (int i = 0; i < s.length; i += 4) {
            coords[c++] = x + s[i] * w + s[i + 1] * arcWidth;
            coords[c++] = y + s[i + 2] * h + s[i + 3] * arcHeight;
        }
        if (transform != null)
            transform.transform(coords, 0, coords, 0, c / 2);
        return type;
    }

    @Override
    public String toString() {
        return "BetterRoundRectIterator(" + x + ", " + y + ", " + w + ", " + h + ", radius=[" +
                topLeftArcWidth + " / " + topLeftArcHeight + ", " +
                topRightArcWidth + " / " + topRightArcHeight + ", " +
                bottomLeftArcWidth + " / " + bottomLeftArcHeight + ", " +
                bottomRightArcWidth + " / " + bottomRightArcHeight + "]";
    }}