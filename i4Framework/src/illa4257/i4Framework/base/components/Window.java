package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;
import illa4257.i4Utils.SyncVar;

public class Window extends Container {// 238
    private final SyncVar<String> title = new SyncVar<>();

    public Window() { super(); visible = false; }

    public void center() { fire(new CenterWindowEvent(this)); }

    public void setTitle(final String newTitle) { fire(new ChangeTextEvent(title.getAndSet(newTitle), newTitle)); }
    public String getTitle() { return title.get(); }
}