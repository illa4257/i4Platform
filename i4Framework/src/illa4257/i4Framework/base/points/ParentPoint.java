package illa4257.i4Framework.base.points;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.math.Orientation;

import java.util.concurrent.atomic.AtomicReference;

public class ParentPoint extends Point {
    private final AtomicReference<Component> component;
    private final AtomicReference<Orientation> orientation;

    private final EventListener<RecalculateEvent> recalc = ignored -> reset();
    private final EventListener<ChangeParentEvent> listener = event -> {
        if (event.oldValue != null)
            event.oldValue.removeEventListener(recalc);
        reset();
        if (event.newValue != null)
            event.newValue.addEventListener(RecalculateEvent.class, recalc);
    };

    public ParentPoint(final Component component, final Orientation orientation) {
        this.component = new AtomicReference<>(component);
        this.orientation = new AtomicReference<>(orientation);
    }

    public void setComponent(final Component newValue) {
        final Component old = component.getAndSet(newValue);
        if (old == newValue)
            return;
        if (isConstructed()) {
            newValue.addEventListener(ChangeParentEvent.class, listener);
            final Container p = newValue.getParent();
            if (p != null)
                p.addEventListener(RecalculateEvent.class, recalc);
        }
        if (old != null) {
            final Container p = old.getParent();
            if (p != null)
                p.removeEventListener(recalc);
            old.removeEventListener(listener);
        }
        reset();
    }

    public void setOrientation(final Orientation newValue) {
        if (orientation.getAndSet(newValue) != newValue)
            reset();
    }

    @Override
    public float calcFloat() {
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
        final Component c = component.get();
        if (c != null) {
            c.addEventListener(ChangeParentEvent.class, listener);
            final Container p = c.getParent();
            if (p != null)
                p.addEventListener(RecalculateEvent.class, recalc);
        }
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        final Component c = component.get();
        if (c != null) {
            c.removeEventListener(listener);
            final Container p = c.getParent();
            if (p != null)
                p.removeEventListener(recalc);
        }
        super.onDestruct();
    }

    @Override
    public String toString() {
        return "ParentSize(" + component.get() + ", mode=" + orientation + ")";
    }
}