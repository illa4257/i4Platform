package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.keyboard.KeyEvent;
import illa4257.i4Framework.base.events.keyboard.KeyPressEvent;
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

    public TextField() {
        setFocusable(true);
        addEventListener(MouseDownEvent.class, e -> {
            index.set(getIndex(e.localX));
            selectionIndex.set(-1);
            if (e.button == MouseButton.BUTTON0)
                md.set(true);
            repaint();
        });
        addEventListener(MouseUpEvent.class, e -> {
            index.set(getIndex(e.localX));
            if (e.button == MouseButton.BUTTON0)
                md.set(false);
            repaint();
        });
        addEventListener(MouseMoveEvent.class, e -> {
            if (md.get()) {
                if (selectionIndex.get() == -1)
                    selectionIndex.set(index.get());
                index.set(getIndex(e.localX));
                if (e.localX <= 8)
                    position.set(Math.max(position.get() - 1, 0));
                else if (e.localX >= width.calcInt() - 8)
                    position.set(Math.min(position.get() + 1, text.size() - 16));
                repaint();
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
                    }
                    selectionIndex.set(-1);
                    repaint();
                    return;
                }
                if (i == 0)
                    return;
                index.set(--i);
                text.remove(i);
                repaint();
                return;
            }
            if (e.keyCode == KeyEvent.DELETE) {
                int i = index.get();
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
                index.set(--i);
                repaint();
                return;
            }
            if (e.keyCode == KeyEvent.RIGHT) {
                final int i = index.get();
                if (i == text.size())
                    return;
                index.set(i + 1);
                repaint();
                return;
            }
            if (e.keyChar == KeyEvent.ENTER || KeyEvent.isNotVisible(e.keyCode))
                return;
            if (e.keyChar >= 1 && e.keyChar <= 26)
                return;
            text.add(e.keyChar, index.getAndIncrement());
            repaint();
        });
    }

    private int getIndex(final int localX) {
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