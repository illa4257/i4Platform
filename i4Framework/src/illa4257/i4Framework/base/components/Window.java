package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.events.components.ChangeTextEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.window.CenterWindowEvent;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.PointSet;
import illa4257.i4Utils.SyncVar;

public class Window extends Container {
    private final SyncVar<String> title = new SyncVar<>();
    public final SyncVar<FrameworkWindow> frameworkWindow = new SyncVar<>();

    public final PointSet
            safeStartX = new PointSet(startX), safeStartY = new PointSet(startY),
            safeEndX = new PointSet(endX), safeEndY = new PointSet(endY);

    public final Point
            safeWidth = new PPointSubtract(safeEndX, safeStartX),
            safeHeight = new PPointSubtract(safeEndY, safeStartY);

    public Window() { super(); visible = false; }

    @Override public Window getWindow() { return this; }

    @Override
    public Framework getFramework() {
        final FrameworkWindow fw = frameworkWindow.get();
        if (fw != null) {
            final Framework f = fw.getFramework();
            if (f != null)
                return f;
        }
        final Container p = parent.get();
        return p != null ? p.getFramework() : null;
    }

    @Override
    protected boolean childFocus(Component targetChild, Component target) {
        synchronized (locker) {
            final FrameworkWindow fw = frameworkWindow.get();
            if (fw != null) {
                if (focused != null)
                    focused.fire(new FocusEvent(false));
                focused = targetChild;
                if (targetChild == target)
                    focused.fire(new FocusEvent(true));
                return true;
            }
            return super.childFocus(targetChild, target);
        }
    }

    public void center() { fire(new CenterWindowEvent(this)); }

    public void setTitle(final String newTitle) { fire(new ChangeTextEvent(title.getAndSet(newTitle), newTitle)); }
    public String getTitle() { return title.get(); }
}