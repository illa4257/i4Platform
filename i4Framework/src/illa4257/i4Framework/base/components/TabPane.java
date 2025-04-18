package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.base.points.PointSet;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TabPane extends Container {
    public final PointSet tabHeight = new PointSet(new PointAttach(32, null));

    public final ConcurrentLinkedQueue<Tab> tabs = new ConcurrentLinkedQueue<>();

    public SyncVar<Tab> current = new SyncVar<>();

    private Context lastContext = null;

    public static class Tab {
        public final AtomicBoolean isCloseable;
        public final SyncVar<String> title;
        public final Component component;

        public Tab(final String title, final Component component) {
            this.title = new SyncVar<>(title);
            this.component = component;
            this.isCloseable = new AtomicBoolean(true);
        }

        public Tab(final String title, final Component component, final boolean isCloseable) {
            this.title = new SyncVar<>(title);
            this.component = component;
            this.isCloseable = new AtomicBoolean(isCloseable);
        }
    }

    public TabPane() {
        setFocusable(true);
        addEventListener(MouseUpEvent.class, e -> {
            final Context ctx = lastContext;
            if (e.y > tabHeight.calcInt() || e.x < 8 || ctx == null)
                return;
            final float xw = ctx.bounds("x").x + 16;
            float x = e.x - 8;
            for (final Tab t : tabs) {
                final boolean isCloseable = t.isCloseable.get();
                final float w = ctx.bounds(t.title.get("Tab")).x + (isCloseable ? 8 + xw : 16);
                if (x < w) {
                    if (isCloseable && w - xw < x)
                        removeTab(t);
                    else
                        selectTab(t);
                    break;
                } else
                    x -= (int) w;
            }
            selectTab(Math.round(x / 130));
        });
    }

    public void addTab(final Tab tab) {
        tabs.add(tab);
        repaint();
    }

    public void selectTab(final Tab tab) {
        if (tab == null)
            return;
        final Tab old;
        if ((old = current.getAndSet(tab)) != tab) {
            tab.component.classes.add("tab-element");
            tab.component.setX(0);
            tab.component.setStartY(tabHeight);
            tab.component.setEndX(width);
            tab.component.setEndY(height);
            if (old != null) {
                old.component.classes.remove("tab-element");
                remove(old.component);
            }
            add(tab.component);
            repaint();
        }
    }

    public boolean selectTab(int i) {
        Tab tab = null;
        for (final Tab t : tabs) {
            if (i == 0) {
                tab = t;
                break;
            }
            i--;
        }
        if (tab == null)
            return false;
        selectTab(tab);
        return true;
    }

    public void removeTab(final Tab tab) {
        if (tabs.remove(tab) && current.get() == tab && !selectTab(0)) {
            tab.component.classes.remove("tab-element");
            remove(tab.component);
            current.setIfEquals(null, current.get());
        }
        repaint();
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        lastContext = context;
        float th = tabHeight.calcFloat();
        final Color tabsBG = getColor("--tabs-background-color"),
                    tabBG = getColor("--tab-background-color"),
                    tabSelectedBG = getColor("--tab-selected-background-color"),
                    color = getColor("color");
        if (tabsBG.alpha > 0) {
            context.setColor(tabsBG);
            context.drawRect(0, 0, width.calcFloat(), th);
        }
        th -= 2;
        final float closeW = context.bounds("x").x;
        float x = 8;
        for (final Tab t : tabs) {
            final boolean isCloseable = t.isCloseable.get();
            final String title = t.title.get("Tab");
            final float tw = context.bounds(title).x + (isCloseable ? closeW + 24 : 16);
            context.setColor(current.get() != t ? tabBG : tabSelectedBG);
            context.drawRect(x, 2, tw, th);
            context.setColor(color);
            context.drawString(title, x + 6, 4);
            x += tw;
            if (isCloseable)
                context.drawString("x", x - closeW - 8, 4);
        }
    }
}