package i4Framework.base.components;

import i4Framework.base.Color;
import i4Framework.base.Context;
import i4Framework.base.HorizontalAlign;
import i4Framework.base.Vector2D;
import i4Framework.base.events.components.ChangeTextEvent;

import java.util.Objects;

public class Button extends Component {
    private String text;
    private Color background = Color.repeat3(200), foreground = Color.BLACK;
    private HorizontalAlign horizontalAlign = HorizontalAlign.CENTER;

    public Button() { this.text = null; }
    public Button(final String text) { this.text = text; }

    public void setText(final String text) {
        final String old;
        synchronized (locker) {
            if (Objects.equals(this.text, text))
                return;
            old = this.text;
            this.text = text;
        }
        fire(new ChangeTextEvent(old, text));
    }

    public void setHorizontalAlign(final HorizontalAlign align) {
        synchronized (locker) {
            this.horizontalAlign = align;
        }
    }

    public void setBackground(final Color newColor) {
        synchronized (locker) {
            background = newColor;
        }
    }

    public void setForeground(final Color newColor) {
        synchronized (locker) {
            foreground = newColor;
        }
    }

    @Override
    public void paint(final Context ctx) {
        ctx.setColor(background);
        ctx.drawRect(0, 0, width.calcFloat(), height.calcFloat());
        //ctx.drawRect(startX.calcFloat(), startY.calcFloat(), width.calcFloat(), height.calcFloat());
        final String t = text;
        if (t == null)
            return;
        final Vector2D s = ctx.bounds(t);
        ctx.setColor(foreground);
        //ctx.drawString(t, startX.calcFloat() + (width.calcFloat() - s.x) / 2, startY.calcFloat() + (height.calcFloat() - s.y) / 2);
        ctx.drawString(t, horizontalAlign == HorizontalAlign.LEFT ? 0 : (width.calcFloat() - s.x) / 2, (height.calcFloat() - s.y) / 2);
    }
}