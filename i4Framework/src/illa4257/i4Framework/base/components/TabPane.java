package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.base.points.PointSet;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.ConcurrentLinkedQueue;

public class TabPane extends Container {
    public final SyncVar<Color> tabPaneBackground = new SyncVar<>(Color.repeat3(200)), tabBackground = new SyncVar<>(Color.repeat3(220)), tabSelectedBackground = new SyncVar<>(Color.repeat3(240)), tabForeground = new SyncVar<>(Color.repeat3(16));
    public final PointSet tabHeight = new PointSet(new PointAttach(32, null));

    public final ConcurrentLinkedQueue<Tab> tabs = new ConcurrentLinkedQueue<>();

    public SyncVar<Tab> current = new SyncVar<>();

    public static class Tab {
        public SyncVar<String> title = new SyncVar<>();
        public final Panel container = new Panel();
    }

    public TabPane() {
        setFocusable(true);
        addEventListener(MouseUpEvent.class, e -> {
            if (e.localY > tabHeight.calcInt())
                return;
            selectTab((e.localX - 4) / 130);
        });
    }

    public void addTab(final Tab tab) {
        tabs.add(tab);
    }

    public void selectTab(final Tab tab) {
        if (tab == null)
            return;
        final Tab old;
        if ((old = current.getAndSet(tab)) != tab) {
            tab.container.setX(0);
            tab.container.setStartY(tabHeight);
            tab.container.setEndX(width);
            tab.container.setEndY(new PPointSubtract(height, tabHeight));
            if (old != null)
                remove(old.container);
            add(tab.container);
        }
    }

    public void selectTab(int i) {
        Tab tab = null;
        for (final Tab t : tabs) {
            if (i == 0) {
                tab = t;
                break;
            }
            i--;
        }
        if (i != 0)
            return;
        selectTab(tab);
    }

    @Override
    public void paint(Context context) {
        context.setColor(tabPaneBackground.get());
        float th = tabHeight.calcFloat();
        context.drawRect(0, 0, width.calcFloat(), th);
        th -= 2;
        float x = 4;
        for (final Tab t : tabs) {
            context.setColor((current.get() == t ? tabSelectedBackground : tabBackground).get());
            context.drawRect(x, 2, 128, th);
            context.setColor(tabForeground.get());
            context.drawString(t.title.get("Tab"), x + 6, 4);
            final float xw = context.bounds("x").x;
            x += 130;
            context.drawString("x", x - 14 - xw, 4);
        }
    }
}