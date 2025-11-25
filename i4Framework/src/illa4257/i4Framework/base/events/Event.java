package illa4257.i4Framework.base.events;

import illa4257.i4Framework.base.components.Component;

public class Event implements IEvent {
    public final boolean isSystem;
    public final int pointerId;
    public final float globalX, globalY, x, y;
    public final Component component;

    public boolean isPrevented = false, isParentPrevented = false;

    public Event(final Component component) { this.component = component; isSystem = false; x = y = globalX = globalY = pointerId = -1; }
    public Event(final Component component, final boolean isSystem) { this.component = component; this.isSystem = isSystem; x = y = globalX = globalY = pointerId = -1; }

    public Event(final Component component, final boolean isSystem, final int pointerId,
                 final float globalX, final float globalY,
                 final float x, final float y) {
        this.component = component;
        this.isSystem = isSystem;
        this.pointerId = pointerId;
        this.globalX = globalX; this.globalY = globalY;
        this.x = x; this.y = y;
    }

    @Override public boolean isSystem() { return isSystem; }
    @Override public boolean isPrevented() { return isPrevented; }
    @Override public boolean isParentPrevented() { return isParentPrevented || isPrevented; }

    public int id() { return pointerId; }
    public float x() { return x; }
    public float y() { return y; }
    public float globalX() { return globalX; }
    public float globalY() { return globalY; }

    public Event prevent(final boolean prevent) { isPrevented = prevent; return this; }
    public Event parentPrevent(final boolean prevent) { isParentPrevented = prevent; return this; }

    @Override public Component getComponent() { return component; }
}