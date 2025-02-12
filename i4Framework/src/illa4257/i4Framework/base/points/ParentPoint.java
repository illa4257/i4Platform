package illa4257.i4Framework.base.points;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.math.Orientation;

import java.util.concurrent.atomic.AtomicReference;

public class ParentPoint extends Point {
    private final AtomicReference<Component> component;
    private final AtomicReference<Orientation> orientation;

    private final EventListener<ChangeParentEvent> listener = ignored -> reset();

    public ParentPoint(final Component component, final Orientation orientation) {
        this.component = new AtomicReference<>(component);
        this.orientation = new AtomicReference<>(orientation);
    }

    public void setComponent(final Component newValue) {
        if (getLinkNumber() > 0)
            newValue.addEventListener(ChangeParentEvent.class, listener);
        final Component old = component.getAndSet(newValue);
        if (old != null)
            old.removeEventListener(listener);
        if (old != newValue)
            reset();
    }

    public void setOrientation(final Orientation newValue) {
        if (orientation.getAndSet(newValue) != newValue)
            reset();
    }

    @Override
    protected float calc() {
        final Component c = component.get();
        if (c == null)
            return 0;
        final Container p = c.getParent();
        if (p == null)
            return 0;
        return (orientation.get() != Orientation.VERTICAL ? p.width : p.height).calcFloat();
    }

    @Override
    public void onConstruct() {
        super.onConstruct();
        final Component c = component.get();
        if (c == null)
            return;
        c.addEventListener(ChangeParentEvent.class, listener);
        reset();
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        final Component c = component.get();
        if (c != null)
            c.removeEventListener(listener);
    }
}