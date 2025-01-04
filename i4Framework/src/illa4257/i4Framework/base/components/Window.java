package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;
import illa4257.i4Framework.base.events.window.ChangeBackgroundEvent;
import illa4257.i4Utils.SyncVar;

public class Window extends Container {
    private final SyncVar<Color> background = new SyncVar<>(Color.repeat3(238));
    private final SyncVar<String> title = new SyncVar<>();

    public Window() { super(); visible = false; }

    public void center() { fire(new CenterWindowEvent(this)); }

    public void setBackground(final Color newColor) {
        background.set(newColor);
        fire(new ChangeBackgroundEvent(this, newColor));
    }

    public Color getBackground() {
        return background.get();
    }

    public void setTitle(final String newTitle) {
        fire(new ChangeTextEvent(title.getAndSet(newTitle), newTitle));
    }

    public String getTitle() {
        return title.get();
    }

    @Override
    public void paint(Context context) {
        context.setColor(background.get());
        context.drawRect(0, 0, width.calcFloat(), height.calcFloat());
    }
}