package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RemoveComponentEvent;
import illa4257.i4Framework.base.points.Point;

public class WidthByContent extends Point {
    public final Container container;

    public WidthByContent(final Container container) {
        this.container = container;
    }

    @Override
    public float calcFloat() {
        float mx = 0;
        for (final Component c : container) {
            final float x = c.endX.calcFloat();
            if (x > mx)
                mx = x;
        }
        return mx;
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
        super.onDestruct();
    }
}