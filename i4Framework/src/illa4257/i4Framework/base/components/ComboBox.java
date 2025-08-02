package illa4257.i4Framework.base.components;

import illa4257.i4Framework.base.Context;
import illa4257.i4Framework.base.events.components.ChangeParentEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.mouse.MouseUpEvent;
import illa4257.i4Framework.base.math.Unit;

/**
 * Not implemented yet
 * TODO: implement
 */
public class ComboBox<T> extends TextField {
    private final Panel optionsPanel = new Panel();

    public ComboBox() {
        addEventListener(MouseUpEvent.class, e -> {
            final Window w = getWindow();
            if (w == null)
                return;
            optionsPanel.setWidth(128, Unit.DP);
            optionsPanel.setHeight(128, Unit.DP);
            w.add(optionsPanel);
        });
        addEventListener(ChangeParentEvent.class, e -> optionsPanel.remove());
        addEventListener(FocusEvent.class, e -> {
            if (!e.value)
                optionsPanel.remove();
        });
    }

    @Override
    public void paint(final Context context) {
        super.paint(context);

    }
}