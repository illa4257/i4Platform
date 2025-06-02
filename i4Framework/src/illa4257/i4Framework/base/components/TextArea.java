package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.SingleEvent;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.keyboard.KeyDownEvent;
import illa4257.i4Framework.base.events.keyboard.KeyEvent;
import illa4257.i4Framework.base.events.keyboard.KeyUpEvent;
import illa4257.i4Framework.base.events.mouse.*;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Utils.lists.MutableCharArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/// WIP
public class TextArea extends Container {
    public final ScrollBar vBar = new ScrollBar(Orientation.VERTICAL), hBar = new ScrollBar(Orientation.HORIZONTAL);
    private volatile Context lastContext = null;

    private final AtomicInteger ctrlCounter = new AtomicInteger(0), shiftCounter = new AtomicInteger(0);

    private final ArrayList<MutableCharArray> lines = new ArrayList<>();
    private final ArrayList<Float> lineWidths = new ArrayList<>();
    private float posX = 0, posY = 0;
    private volatile int lineY = 0, lineX = 0, lineStartY = -1;
    private boolean mouseLeft = false;

    public TextArea() {
        setFocusable(true);
        vBar.setStartX(vBar.endX);
        vBar.setEndX(width);
        vBar.setEndY(hBar.startY);
        add(vBar);

        hBar.setStartY(hBar.endY);
        hBar.setEndX(vBar.startX);
        hBar.setEndY(height);
        add(hBar);

        addEventListener(ReCalc.class, e -> reCalc());
        addEventListener(RecalculateEvent.class, e -> fire(new ReCalc()));
        vBar.addEventListener(ScrollBar.ScrollEvent.class, e -> {
            synchronized (lines) {
                posY = e.newValue;
                repaint();
            }
        });
        hBar.addEventListener(ScrollBar.ScrollEvent.class, e -> {
            synchronized (lines) {
                posX = e.newValue;
                repaint();
            }
        });
        addEventListener(MouseDownEvent.class, e -> {
            if (e.button != MouseButton.BUTTON0)
                return;
            mouseLeft = true;
            final Context c = lastContext;
            lineStartY = -1;
            lineY = getLineY(c, e.y());
            lineX = getCursorX(c, e.x(), lineY);
            repaint();
        });
        addEventListener(MouseMoveEvent.class, e -> {
            if (!mouseLeft)
                return;
            final Context c = lastContext;
            if (lineStartY == -1)
                lineStartY = lineY;
            lineY = getLineY(c, e.y());
            lineX = getCursorX(c, e.x(), lineY);
            repaint();
        });
        addEventListener(MouseUpEvent.class, e -> {
            if (e.button == MouseButton.BUTTON0)
                mouseLeft = false;
        });
        addEventListener(MouseScrollEvent.class, e -> {
            final ScrollBar bar = e.orientation == Orientation.VERTICAL ? vBar : hBar;
            if (
                    e.scroll == 0 ||
                            (bar.getMin() == bar.getScroll() && e.scroll < 0) ||
                            (bar.getMax() == bar.getScroll() && e.scroll > 0)
            )
                return;
            bar.fire(e);
        });
        addEventListener(ChangeParentEvent.class, e -> {
            ctrlCounter.set(0);
            shiftCounter.set(0);
        });
        addEventListener(KeyDownEvent.class, e -> {
            switch (e.keyCode) {
                case KeyEvent.CTRL:
                    ctrlCounter.incrementAndGet();
                    break;
                case KeyEvent.SHIFT:
                    shiftCounter.incrementAndGet();
                    break;
            }
        });
        addEventListener(KeyUpEvent.class, e -> {
            switch (e.keyCode) {
                case KeyEvent.CTRL:
                    ctrlCounter.decrementAndGet();
                    break;
                case KeyEvent.SHIFT:
                    shiftCounter.decrementAndGet();
                    break;
            }
        });
    }

    private int getLineY(final Context ctx, final float cursorY) {
        return Math.max(Math.min((int) Math.floor((posY + cursorY) / ctx.bounds(new char[] { 'H' }).y), lines.size() - 1), 0);
    }

    private int getCursorX(final Context ctx, final float cursorX, final int lineY) {
        final MutableCharArray arr = lines.get(lineY);
        final float cx = cursorX + posX;
        float x = ctx.bounds(Integer.toString(lines.size())).x +
                calcStyleNumber("--gutter-padding", Orientation.HORIZONTAL, 0) * 3;
        int i = 0;
        Character ch = arr.getChar(i, null);
        if (ch != null) {
            while (cx > x) {
                x += ctx.charWidth(ch);
                i++;
                ch = arr.getChar(i, null);
                if (ch == null)
                    break;
            }
            if (ch != null && i > 0 && cx < x - ctx.charWidth(arr.getChar(i - 1)) / 2)
                i--;
        }
        return i;
    }

    private Float getLineWidth(final int index) {
        synchronized (lines) {
            Float r = lineWidths.get(index);
            if (r == null) {
                final Context c = lastContext;
                if (c != null) {
                    final char[] b = lines.get(index).getChars();
                    r = c.bounds(b).x;
                    Arrays.fill(b, '\0');
                    lineWidths.set(index, r);
                }
            }
            return r;
        }
    }

    public void addLine(final char[] chars) {
        final MutableCharArray arr = new MutableCharArray();
        arr.addDirect(chars);
        synchronized (lines) {
            lines.add(arr);
            lineWidths.add(null);
        }
    }

    private void reCalc() {
        final Context c = lastContext;
        if (c == null) {
            fireLater(new ReCalc());
            return;
        }
        final int scrollBarWidth = getInt("--scrollbar-width", 0);
        synchronized (lines) {
            int m = Math.round(Math.max(lines.size() * c.bounds(new char[] { 'H' }).y - height.calcFloat() + scrollBarWidth, 0));
            vBar.setMax(m);
            vBar.setStartX(m > 0 ? new PointAttach(-scrollBarWidth, vBar.endX) : vBar.endX);

            final int l = lines.size();
            float r = 0;
            for (int i = 0; i < l; i++) {
                final float w = getLineWidth(i);
                if (w > r)
                    r = w;
            }
            m = Math.round(Math.max(r - width.calcFloat() + scrollBarWidth + 40 + c.bounds(Integer.toString(lines.size())).x, 0));
            hBar.setMax(m);
            hBar.setStartY(m > 0 ? new PointAttach(-scrollBarWidth, hBar.endY) : hBar.endY);
        }
    }

    private static class ReCalc implements SingleEvent {
        @Override
        public boolean isPrevented() {
            return false;
        }
    }

    /**
     * Clears input.<br>
     * Use this method after use if it was used for sensitive data.
     */
    public void clear() {
        synchronized (lines) {
            for (final MutableCharArray arr : lines)
                arr.clear();
            lines.clear();
            lineWidths.clear();
        }
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        lastContext = context;

        Color col = getColor("color");
        if (col.alpha <= 0)
            return;
        context.setColor(col);
        synchronized (lines) {
            final char[] buff = new char[] { 'H' };
            final float w = width.calcFloat(), h = height.calcFloat(), textHeight = context.bounds(buff).y,
                        gutterPaddingX = calcStyleNumber("--gutter-padding", Orientation.HORIZONTAL, 0);
            final int l = Math.min(lines.size(), (int) Math.ceil((posY + h) / textHeight));
            final float lineNumberEnd = context.bounds(Integer.toString(lines.size())).x + gutterPaddingX,
                    lineNumberWidth = lineNumberEnd + gutterPaddingX;
            buff[0] = 'W';
            float y = -posY + (int) Math.floor(posY / textHeight) * textHeight, x, chw;
            int i;
            for (int lineIndex = (int) Math.floor(posY / textHeight); lineIndex < l && y < h; lineIndex++, y += textHeight) {
                x = gutterPaddingX;
                final MutableCharArray line = lines.get(lineIndex);
                if (lineIndex == lineY) {
                    final Color curLineCol = getColor("--current-line-background-color");
                    if (curLineCol.alpha > 0) {
                        context.setColor(curLineCol);
                        context.drawRect(0, y, w, textHeight);
                        context.setColor(col);
                    }
                }
                i = 0;
                x -= posX;
                while (true) {
                    final Character ch = line.getChar(i, null);
                    if (ch == null)
                        break;
                    chw = context.charWidth(ch);
                    if (x > -chw)
                        break;
                    x += chw;
                    i++;
                }
                x += lineNumberWidth;
                for (; x < w; i++) {
                    if (lineY == lineIndex && lineX == i)
                        context.drawRect(x, y, 2, textHeight);
                    final Character ch = line.getChar(i, null);
                    if (ch == null)
                        break;
                    buff[0] = ch;
                    context.drawString(buff, x, y);
                    x += context.charWidth(ch);
                }
            }
            col = getColor("--gutter-background-color");
            if (col.alpha > 0) {
                context.setColor(col);
                context.drawRect(0, 0, lineNumberWidth, h);
            }
            col = getColor("--gutter-color");
            if (col.alpha <= 0)
                return;
            context.setColor(col);
            y = -posY + (int) Math.floor(posY / textHeight) * textHeight;
            for (int lineIndex = (int) Math.floor(posY / textHeight); lineIndex < l && y < h; y += textHeight)
                context.drawString(Integer.toString(++lineIndex), lineNumberEnd - context.bounds(Integer.toString(lineIndex)).x, y);
            context.drawLine(lineNumberWidth, 0, lineNumberWidth, h);
        }
    }

    private void reCalcRequest() {
        fire(new ReCalc());
    }

    @Override
    public void onConstruct() {
        super.onConstruct();
        densityMultiplier.subscribe(this::reCalcRequest);
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        densityMultiplier.unsubscribe(this::reCalcRequest);
    }
}