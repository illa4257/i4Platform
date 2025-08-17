package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.ActionEvent;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.mouse.MouseDownEvent;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.points.ops.PPointMin;

import static illa4257.i4Framework.base.math.Unit.DP;

/**
 * Not implemented yet.<br>
 * TODO: implement
 */
public class ComboBox<T> extends TextField {
    private final ScrollPane optionsPane = new ScrollPane();
    private volatile Context ctx;
    public volatile Iterable<T> options = null;
    private volatile T selected = null;

    public ComboBox() {
        optionsPane.classes.add("combobox-options");
        addEventListener(MouseDownEvent.class, e -> showOptions());
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

    private void showOptions() {
        final Window w = getWindow();
        final Context c = ctx;
        final Iterable<T> iter = options;
        if (w == null || c == null)
            return;
        optionsPane.setStartX(windowStartX);
        optionsPane.setWidth(width);

        final Container op = new Container();
        op.setWidth(optionsPane.viewableWidth);
        boolean hasOptions = false;
        if (iter != null) {
            Point ly = null;
            for (final T o : iter) {
                final Button btn = new Button(o);
                btn.setStartY(ly);
                btn.setEndX(op.width);
                btn.setHeight(24, DP);
                btn.addEventListener(ActionEvent.class, e -> {
                    text.clear();
                    text.add(String.valueOf(o));
                    optionsPane.remove();
                    selected = o;
                    index.set(text.size());
                    requestFocus();
                    repaint();
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

        /* TODO: Differentiate when to top
        optionsPane.setStartY(new PPointSubtract(optionsPane.endY, new PPointMin(op.height, new NumberPointMultiplier(256, densityMultiplier))));
        optionsPane.setEndY(new PointAttach(-calcStyleNumber("border-width", Orientation.VERTICAL, 0), windowStartY));
        */

        optionsPane.setStartY(new PointAttach(calcStyleNumber("border-width", Orientation.VERTICAL, 0), windowEndY));
        optionsPane.setHeight(new PPointMin(op.height, new NumberPointMultiplier(256, densityMultiplier)));

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