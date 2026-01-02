package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;
import illa4257.i4Framework.base.points.Point;

public class HeightByContent extends Point {
    public static final HeightByContent ZERO = new HeightByContent(0, null);

    public final float value;
    public final Container container;

    public HeightByContent(final float value, final Container container) {
        this.value = value;
        this.container = container;
    }

    @Override
    protected float calc() {
        float my = 0;
        for (final Component c : container) {
            final float y = c.endY.calcFloat();
            if (y > my)
                my = y;
        }
        return my + value;
    }

    private void r(final RecalculateEvent e) {
        final Container c = e.component.getParent();
        if (c != container)
            return;
        reset();
    }

    private void ac(final AddComponentEvent e) {
        if (e.container != container)
            return;
        reset();
        e.child.addEventListener(RecalculateEvent.class, this::r);
    }

    private void rc(final RemoveComponentEvent e) {
        if (e.container != container)
            return;
        reset();
        e.child.removeEventListener(this::r);
    }

    @Override
    public void onConstruct() {
        container.addEventListener(AddComponentEvent.class, this::ac);
        container.addEventListener(RemoveComponentEvent.class, this::rc);
        for (final Component c : container)
            c.addEventListener(RecalculateEvent.class, this::r);
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        container.removeEventListener(this::ac);
        container.removeEventListener(this::rc);
        for (final Component c : container)
            c.removeEventListener(this::r);
    }
}