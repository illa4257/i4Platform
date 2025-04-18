package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;
import illa4257.i4Framework.base.points.Point;

public class PointByContent extends Point {
    public static final PointByContent ZERO = new PointByContent(0, null);

    public final float value;
    public final Container container;

    public PointByContent(final float value, final Container container) {
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

    private void clChange(final Event e) { reset(); }

    @Override
    public void onConstruct() {
        container.addEventListener(AddComponentEvent.class, this::clChange);
        container.addEventListener(RemoveComponentEvent.class, this::clChange);
        super.onConstruct();
    }

    @Override
    public void onDestruct() {
        container.removeEventListener(this::clChange);
    }
}