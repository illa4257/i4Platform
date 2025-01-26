package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.input.MouseDownEvent;
import illa4257.i4Utils.lists.MutableCharArray;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TextField extends Component {
    public final AtomicBoolean hideCharacters = new AtomicBoolean(false);
    public final AtomicInteger index = new AtomicInteger(0), selectionIndex = new AtomicInteger(-1), position = new AtomicInteger(0);
    public final MutableCharArray text = new MutableCharArray();

    public TextField() {
        addEventListener(FocusEvent.class, e -> {
            if (e.value)
                repaint();
        });
        addEventListener(MouseDownEvent.class, ignored -> requestFocus());
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

        {
            final Color c = getColor("color");
            if (c.alpha <= 0)
                return;
            context.setColor(c);
        }

        float x = 8;
        int i = position.get();
        if (i > 0) {
            x = 0;
            i -= 1;
        }

        final char[] arr = new char[] { 'H' };
        final float w = width.calcFloat(), th = context.bounds(arr).y, y = (height.calcFloat() - th) / 2;
        final boolean isF = isFocused();
        if (hideCharacters.get()) {
            arr[0] = '*';
            final float sw = context.bounds(arr).x;
            for (; x < w; i++) {
                if (text.getChar(i, null) == null)
                    break;
                if (isF && i == index.get())
                    context.drawRect(x, y, 2, th);
                context.drawString(arr, x, y);
                x += sw;
            }
            return;
        }
        for (; x < w; i++) {
            final Character ch = text.getChar(i, null);
            if (ch == null)
                break;
            if (isF && i == index.get())
                context.drawRect(x, y, 2, th);
            arr[0] = ch;
            context.drawString(arr, x, y);
            x += context.bounds(arr).x;
        }
    }
}