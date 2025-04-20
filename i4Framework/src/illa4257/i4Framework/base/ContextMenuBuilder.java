package illa4257.i4Framework.base;

import illa4257.i4Framework.base.components.*;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.math.Unit;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.layout.PointByContent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ContextMenuBuilder {
    public final Framework framework;
    public final ConcurrentLinkedQueue<ContextMenuItem> items = new ConcurrentLinkedQueue<>();

    public ContextMenuBuilder(final Framework framework) {
        this.framework = framework;
    }

    public ContextMenuBuilder add(final ContextMenuItem item) {
        items.add(item);
        return this;
    }

    public ContextMenuBuilder addButton(final String name, final Runnable runnable) {
        items.add(new ContextMenuButton(name, runnable));
        return this;
    }

    public void build() {
        Point last = null;
        final Window window = new Window();
        final FrameworkWindow fw = framework.newWindow(window);
        window.classes.add("context-menu");
        for (final ContextMenuItem item : items)
            if (item instanceof ContextMenuButton) {
                final Button btn = new Button(((ContextMenuButton) item).name);
                btn.addEventListener(ActionEvent.class, e -> {
                    fw.dispose();
                    ((ContextMenuButton) item).run();
                });
                btn.setStartY(last);
                btn.setWidth(window.width);
                btn.setHeight(24, Unit.DP);
                last = btn.endY;
                window.add(btn);
            }
        window.setWidth(256, Unit.DP);
        window.setHeight(new PointByContent(0, window));
        window.setFocusable(true);
        fw.asContextMenu();
        window.setVisible(true);
    }

    public static class ContextMenuItem {}
    public static class ContextMenuButton extends ContextMenuItem implements Runnable {
        public final String name;
        public final Runnable runnable;

        public ContextMenuButton(final String name) {
            this.name = name;
            this.runnable = null;
        }

        public ContextMenuButton(final String name, final Runnable runnable) {
            this.name = name;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            if (runnable != null)
                runnable.run();
        }
    }
}