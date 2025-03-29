package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.keyboard.KeyDownEvent;
import illa4257.i4Framework.base.events.keyboard.KeyEvent;
import illa4257.i4Framework.base.events.keyboard.KeyPressEvent;
import illa4257.i4Framework.base.events.keyboard.KeyUpEvent;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.mouse.MouseButton;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.mouse.MouseMoveEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.lists.MutableCharArray;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TextField extends Component {
    public final AtomicBoolean hideCharacters = new AtomicBoolean(false);
    public final AtomicInteger index = new AtomicInteger(0), selectionIndex = new AtomicInteger(-1), position = new AtomicInteger(0);
    public final MutableCharArray text = new MutableCharArray();
    private final SyncVar<Context> lastContext = new SyncVar<>();

    private final AtomicBoolean md = new AtomicBoolean();

    private static final int additionalCharacters = 4, areaOffset = 16, areaSize = areaOffset * 2;
    private final AtomicInteger ctrlCounter = new AtomicInteger(0), shiftCounter = new AtomicInteger(0);

    public TextField() {
        setFocusable(true);
        addEventListener(MouseDownEvent.class, e -> {
            index.set(getIndex(e.x));
            selectionIndex.set(-1);
            if (e.button == MouseButton.BUTTON0)
                md.set(true);
            repaint();
        });
        addEventListener(MouseUpEvent.class, e -> {
            index.set(getIndex(e.x));
            if (e.button == MouseButton.BUTTON0)
                md.set(false);
            repaint();
        });
        addEventListener(MouseMoveEvent.class, e -> {
            if (md.get()) {
                if (selectionIndex.get() == -1)
                    selectionIndex.set(index.get());
                index.set(getIndex(e.x));
                if (e.x <= areaOffset)
                    position.set(Math.max(position.get() - 1, 0));
                else if (e.x >= width.calcInt() - areaOffset)
                    position.set(Math.min(position.get() + 1, text.size() - additionalCharacters));
                repaint();
            }
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
        addEventListener(KeyPressEvent.class, e -> {
            if (e.keyCode == KeyEvent.BACKSPACE) {
                int i = index.get();
                int si = selectionIndex.get();
                if (si != -1) {
                    if (si > i)
                        text.removeRange(i, si);
                    else {
                        text.removeRange(si, i);
                        index.set(si);
                        if (index.get() < position.get())
                            position.set(index.get());
                    }
                    selectionIndex.set(-1);
                    repaint();
                    return;
                }
                if (i == 0)
                    return;
                index.set(--i);
                text.remove(i);
                if (position.get() >= index.get())
                    position.set(Math.max(position.get() - 1, 0));
                repaint();
                return;
            }
            if (e.keyCode == KeyEvent.DELETE) {
                int i = index.get();
                int si = selectionIndex.get();
                if (si != -1) {
                    if (si > i)
                        text.removeRange(i, si);
                    else {
                        text.removeRange(si, i);
                        index.set(si);
                        if (index.get() < position.get())
                            position.set(index.get());
                    }
                    selectionIndex.set(-1);
                    repaint();
                    return;
                }
                if (i == 0)
                    return;
                text.removeB(i);
                repaint();
                return;
            }
            if (e.keyCode == KeyEvent.LEFT) {
                int i = index.get();
                if (i == 0)
                    return;
                if (shiftCounter.get() > 0) {
                    final int si = selectionIndex.get();
                    if (si == -1)
                        selectionIndex.set(i);
                    else if (si == i - 1)
                        selectionIndex.set(-1);
                } else if (selectionIndex.getAndSet(-1) != -1) {
                    repaint();
                    return;
                }
                index.set(--i);
                if (position.get() >= i)
                    position.set(Math.max(position.get() - 1, 0));
                repaint();
                return;
            }
            if (e.keyCode == KeyEvent.RIGHT) {
                int i = index.get();
                if (text.getChar(i, null) == null)
                    return;
                if (shiftCounter.get() > 0) {
                    final int si = selectionIndex.get();
                    if (si == -1)
                        selectionIndex.set(i);
                    else if (si == i + 1)
                        selectionIndex.set(-1);
                } else if (selectionIndex.getAndSet(-1) != -1) {
                    repaint();
                    return;
                }
                index.set(++i);
                final Context c = lastContext.get();
                if (c != null) {
                    float w = width.calcFloat() - areaSize, cw;
                    final char[] arr = new char[1];
                    int p = position.get();
                    while (true) {
                        final Character ch = text.getChar(p, null);
                        if (ch == null)
                            break;
                        arr[0] = ch;
                        cw = c.bounds(arr).x;
                        if (w < cw)
                            position.incrementAndGet();
                        if (i < p)
                            break;
                        w -= cw;
                        p++;
                    }
                }
                repaint();
                return;
            }
            if (e.keyChar == KeyEvent.ENTER || KeyEvent.isNotVisible(e.keyCode))
                return;
            if (e.keyChar == 1) {
                final int l = text.size();
                if (l == 0)
                    return;
                selectionIndex.set(0);
                index.set(l);
                repaint();
                return;
            }
            if (e.keyChar >= 2 && e.keyChar <= 26)
                return;
            int i = index.get();
            int si = selectionIndex.get();
            if (si != -1) {
                if (si > i) {
                    text.removeRange(i, si);
                } else {
                    text.removeRange(si, i);
                    i = si;
                    if (index.get() < position.get())
                        position.set(index.get());
                }
                selectionIndex.set(-1);
                index.set(i + 1);
            } else
                i = index.getAndIncrement();
            if (i < 0) {
                index.set(i = 0);
                position.set(0);
                text.add(e.keyChar);
            } else
                text.add(e.keyChar, i);
            final Context c = lastContext.get();
            if (c != null) {
                float w = width.calcFloat() - areaSize, cw;
                final char[] arr = new char[1];
                int p = position.get();
                while (true) {
                    final Character ch = text.getChar(p, null);
                    if (ch == null)
                        break;
                    arr[0] = ch;
                    cw = c.bounds(arr).x;
                    if (w < cw)
                        position.incrementAndGet();
                    if (i < p)
                        break;
                    w -= cw;
                    p++;
                }
            }
            repaint();
        });
    }

    private int getIndex(final float localX) {
        final Context context = lastContext.get();
        if (context == null)
            return 0;

        float x = 8;
        int i = position.get();
        if (i > 0) {
            x = 0;
            i -= 1;
        }

        final char[] arr = new char[1];
        final float w = width.calcFloat();
        if (hideCharacters.get()) {
            arr[0] = '*';
            final float sw = context.bounds(arr).x, hw = sw / 2;
            for (; x < w; i++) {
                if (text.getChar(i, null) == null)
                    break;
                if (x + hw >= localX)
                    return i;
                x += sw;
            }
            return i;
        }
        float cw;
        for (; x < w; i++) {
            final Character ch = text.getChar(i, null);
            if (ch == null)
                break;
            arr[0] = ch;
            cw = context.bounds(arr).x;
            if (x + cw / 2 >= localX)
                return i;
            x += cw;
        }
        return i;
    }

    public void setText(final char[] text) {
        this.text.clear();
        index.set(0);
        position.set(0);
        selectionIndex.set(-1);
        this.text.add(text);
    }

    public void setText(final String text) {
        this.setText(text.toCharArray());
    }

    /**
     * Clears input.<br>
     * Use this method after use if it was used for sensitive data.
     */
    public void clear() {
        text.clear();
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        lastContext.set(context);

        final Color textColor = getColor("color");
        if (textColor.alpha <= 0)
            return;
        context.setColor(textColor);

        float x = 8;
        int i = position.get();
        if (i > 0) {
            x = 0;
            i -= 1;
        }

        final int si = selectionIndex.get(), startIndex = index.get();
        final char[] arr = new char[] { 'H' };
        final float w = width.calcFloat(), th = context.bounds(arr).y, y = (height.calcFloat() - th) / 2;
        final boolean isF = isFocused();
        float selectBeginX = -1, selectEndX = -1;
        if (hideCharacters.get()) {
            arr[0] = '*';
            final float sw = context.bounds(arr).x;
            for (; x < w; i++) {
                if (text.getChar(i, null) == null)
                    break;
                if (si == i)
                    selectBeginX = x;
                if (i == startIndex)
                    selectEndX = x;
                context.drawString(arr, x, y);
                x += sw;
            }
        } else
            for (; x < w; i++) {
                final Character ch = text.getChar(i, null);
                if (ch == null)
                    break;
                if (si == i)
                    selectBeginX = x;
                if (i == startIndex)
                    selectEndX = x;
                arr[0] = ch;
                context.drawString(arr, x, y);
                x += context.bounds(arr).x;
            }
        if (selectEndX == -1 && i == startIndex)
            selectEndX = x;

        if (si == -1 || si == startIndex) {
            if (isF && startIndex <= i)
                context.drawRect(selectEndX, y, 2, th);
            return;
        }
        context.setColor(getColor("--selection-color"));
        if (selectBeginX == -1)
            selectBeginX = si > startIndex ? x : 0;
        if (selectEndX == -1)
            selectEndX = x;
        if (selectBeginX > selectEndX) {
            final float t = selectBeginX;
            selectBeginX = selectEndX;
            selectEndX = t;
        }
        context.drawRect(selectBeginX, y, selectEndX - selectBeginX, th);
        context.setColor(textColor);
        context.drawRect(si > startIndex ? selectBeginX : selectEndX, y, 2, th);
    }
}