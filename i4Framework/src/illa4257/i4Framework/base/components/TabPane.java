package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.Orientation;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.MiniUtil;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Utils.SyncVar;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TabPane extends Container {
    public final ConcurrentLinkedQueue<Tab> tabs = new ConcurrentLinkedQueue<>();

    public SyncVar<Tab> current = new SyncVar<>();

    private volatile Context lastContext = null;

    public int getSelectedIndex() { return MiniUtil.indexOf(current.get(), tabs); }

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

            final PropIter ss = getPI();
            ss.select("--tab-height", StyleProperty.pxFilter).nextLayer().nextSet();
            if (e.component != this || e.y > ss.f(Orientation.VERTICAL, 0) || e.x < 8 || ctx == null)
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
        });
    }

    public void addTab(final Tab tab) {
        tabs.add(tab);
        repaint();
    }

    private void selectTab0(final Tab tab) {
        tab.component.classes.add("tab-element");
        tab.component.setX(0);

        final PropIter ss = getPI();
        ss.select("--tab-height", StyleProperty.pxFilter).nextLayer().nextSet();
        tab.component.setStartY(ss.point(Orientation.VERTICAL, NumberPointConstant.ZERO));
        tab.component.setEndX(width);
        tab.component.setEndY(height);
        add(tab.component);
        repaint();
    }

    public void selectTab(final Tab tab) {
        if (tab == null)
            return;
        final Tab old;
        if ((old = current.getAndSet(tab)) != tab) {
            if (old != null) {
                old.component.classes.remove("tab-element");
                remove(old.component);
            }
            if (getFramework() == null)
                invokeLater(() -> selectTab0(tab));
            else
                selectTab0(tab);
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
        final PropIter ss = getPI();

        float th;
        final Paint tabsBG, tabBG, tabSelectedBG, color;

        ss.select("--tab-height", StyleProperty.pxFilter).nextLayer().nextSet();
        th = ss.f(Orientation.VERTICAL, 0);

        ss.select("--tabs-background-color", StyleProperty.paintFilter).nextLayer().nextSet();
        tabsBG = ss.paint(Color.TRANSPARENT);

        ss.select("--tab-background-color", StyleProperty.paintFilter).nextLayer().nextSet();
        tabBG = ss.paint(Color.TRANSPARENT);

        ss.select("--tab-selected-background-color", StyleProperty.paintFilter).nextLayer().nextSet();
        tabSelectedBG = ss.paint(Color.TRANSPARENT);

        ss.select("color", StyleProperty.paintFilter).nextLayer().nextSet();
        color = ss.paint(Color.TRANSPARENT);

        if ((!(tabsBG instanceof Color)) || ((Color) tabsBG).alpha > 0) {
            context.setPaint(tabsBG);
            context.fillRect(0, 0, width.calcFloat(), th);
        }
        th -= 2;
        final float closeW = context.bounds("x").x;
        float x = 8;
        for (final Tab t : tabs) {
            final boolean isCloseable = t.isCloseable.get();
            final String title = t.title.get("Tab");
            final float tw = context.bounds(title).x + (isCloseable ? closeW + 24 : 16);
            context.setPaint(current.get() != t ? tabBG : tabSelectedBG);
            context.fillRect(x, 2, tw, th);
            context.setPaint(color);
            context.drawString(title, x + 6, 4);
            x += tw;
            if (isCloseable)
                context.drawString("x", x - closeW - 8, 4);
        }
    }
}