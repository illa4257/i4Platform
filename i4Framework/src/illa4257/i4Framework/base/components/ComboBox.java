package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchDownEvent;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.media.Color;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.points.ops.PPointMin;

import java.util.function.Function;

import static illa4257.i4Framework.base.math.Unit.DP;

public class ComboBox<T> extends TextField {
    private final ScrollPane optionsPane = new ScrollPane();
    private volatile Context ctx;
    public volatile Iterable<T> options = null;
    public volatile Function<T, String> formatter = String::valueOf;
    private volatile T selected = null;

    private final Point contextHeight = new NumberPointMultiplier(256, densityMultiplier);

    public ComboBox() {
        optionsPane.classes.add("combobox-options");
        addEventListener(MouseDownEvent.class, e -> showOptions());
        addEventListener(TouchDownEvent.class, e -> showOptions());
        addEventListener(ChangeParentEvent.class, e -> optionsPane.remove());
        addEventListener(FocusEvent.class, e -> {
            if (!e.value) {
                if (optionsPane.isFocusedWithin())
                    return;
                optionsPane.remove();
            }
        });
    }

    public T getSelected() { return selected; }

    public void select(final int index) {
        final Iterable<T> opts = options;
        T s = null;
        if (opts != null)
            try {
                s = MiniUtil.get(opts, index);
            } catch (final IndexOutOfBoundsException ignored) {}
        selected = s;
        selectionIndex.set(s != null ? index : 0);
        final Function<T, String> format = formatter;
        text.clear();
        text.add(format != null ? format.apply(s) : String.valueOf(s));
        repaint();
    }

    public void select(final T element) {
        selected = element;
        final Iterable<T> opts = options;
        selectionIndex.set(element != null && opts != null ? MiniUtil.indexOf(element, opts) : 0);
        final Function<T, String> format = formatter;
        text.clear();
        if (element != null)
            text.add(format != null ? format.apply(element) : String.valueOf(element));
        repaint();
    }

    private void showOptions() {
        final Window w = getWindow();
        final Context c = ctx;
        final Iterable<T> iter = options;
        if (w == null || c == null)
            return;
        Function<T, String> f = formatter;
        if (formatter == null)
            f = String::valueOf;
        optionsPane.setStartX(windowStartX);
        optionsPane.setWidth(width);

        final Container op = new Container();
        op.setWidth(optionsPane.viewableWidth);
        boolean hasOptions = false;
        if (iter != null) {
            final Function<T, String> format = f;
            Point ly = null;
            for (final T o : iter) {
                final Button btn = new Button(format.apply(o));
                btn.setStartY(ly);
                btn.setEndX(op.width);
                btn.setHeight(24, DP);
                btn.addEventListener(ActionEvent.class, e -> {
                    text.clear();
                    text.add(format.apply(o));
                    optionsPane.remove();
                    selected = o;
                    index.set(text.size());
                    requestFocus();
                    repaint();
                    fire(new ActionEvent(this));
                });
                op.add(btn);
                ly = btn.endY;
                hasOptions = true;
            }
            if (hasOptions)
                op.setHeight(ly);
        }

        if (!hasOptions) {
            final Label noOptions = new Label("No options");
            noOptions.setEndX(op.width);
            noOptions.setHeight(24, DP);
            op.setHeight(noOptions.height);
            op.add(noOptions);
        }

        if (getWindow().safeEndY.calcFloat() >= windowEndY.calcFloat() + contextHeight.calcFloat()) {
            optionsPane.setStartY(new PointAttach(calcStyleNumber("border-width", Orientation.VERTICAL, 0), windowEndY));
            optionsPane.setHeight(new PPointMin(op.height, contextHeight));
        } else {
            optionsPane.setStartY(new PPointSubtract(optionsPane.endY, new PPointMin(op.height, contextHeight)));
            optionsPane.setEndY(new PointAttach(-calcStyleNumber("border-width", Orientation.VERTICAL, 0), windowStartY));
        }

        optionsPane.setContent(op);

        w.add(optionsPane);
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);
        ctx = context;
        final Color c = getColor("color");
        if (c.alpha <= 0)
            return;
        final boolean a = optionsPane.getParent() != null;
        final float h = height.calcFloat(),
                o = h / 3, szX = h - o * 2, szY = szX / 2, oY = (h - szY) / 2, ey = oY + szY,
                ex = width.calcFloat() - o, cx = ex - szX / 2, sx = ex - szX,
                p1 = a ? ey : oY, p2 = a ? oY : ey;
        context.setColor(c);
        context.drawLine(sx, p1, cx, p2);
        context.drawLine(ex, p1, cx, p2);
    }
}