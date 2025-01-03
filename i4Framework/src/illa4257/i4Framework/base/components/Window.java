package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;
import illa4257.i4Framework.base.events.window.ChangeBackgroundEvent;
import illa4257.i4Utils.SyncVar;

public class Window extends Container {
    private Color background = new Color(238, 238, 238);
    private final SyncVar<String> title = new SyncVar<>();

    public Window() { super(); visible = false; }

    public void center() { fire(new CenterWindowEvent(this)); }

    public void setBackground(final Color newColor) {
        synchronized (locker) {
            background = newColor;
        }
        fire(new ChangeBackgroundEvent(this, newColor));
    }

    public Color getBackground() {
        synchronized (locker) {
            return background;
        }
    }

    public void setTitle(final String newTitle) {
        fire(new ChangeTextEvent(title.getAndSet(newTitle), newTitle));
    }

    public String getTitle() {
        return title.get();
    }

    @Override
    public void paint(Context context) {
        synchronized (locker) {
            if (background == null)
                return;
            context.setColor(background);
        }
        context.drawRect(0, 0, width.calcFloat(), height.calcFloat());
    }
}