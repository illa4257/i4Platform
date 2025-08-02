package illa4257.i4Framework.base.points.layout;

import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.IEvent;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.points.Point;

public class AutoWidth extends Point {
    public final Container container;
    public volatile Point gap;

    public AutoWidth(final Container container) { this.container = container; this.gap = null; }
    public AutoWidth(final Container container, final Point gap) { this.container = container; this.gap = gap; }

    @Override
    protected float calc() {
        final Point p = gap;
        final float g = p != null ? p.calcFloat() : 0;
        final int n = container.getComponentCount();
        return (container.width.calcFloat() - g) / n - g;
    }

    private void onChangeAmount(final IEvent e) {
        reset();
    }

    @Override
    public void onConstruct() {
        super.onDestruct();
        container.width.subscribe(this::reset);
        container.addEventListener(AddComponentEvent.class, this::onChangeAmount);
    }

    @Override
    public void onDestruct() {
        super.onDestruct();
        container.width.unsubscribe(this::reset);
        container.removeEventListener(this::onChangeAmount);
    }
}