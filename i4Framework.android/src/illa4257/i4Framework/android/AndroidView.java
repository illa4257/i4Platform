package illa4257.i4Framework.android;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;
import illa4257.i4Framework.base.math.Orientation;
import illa4257.i4Framework.base.styling.StyleSetting;

public class AndroidView extends ViewGroup {
    public final Component component;

    private volatile int offset = 0;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final EventListener<?>[] listeners;

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {}

    public AndroidView(final Component component, Context context, final boolean isNotRoot) {
        super(context);
        setWillNotDraw(false);
        this.component = component;
        this.context = new AndroidGContext();
        setFocusable(component.isFocusable());
        {
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(component.width.calcInt(), component.height.calcInt());
            lp.leftMargin = component.startX.calcInt();
            lp.topMargin = component.startY.calcInt();
            setLayoutParams(lp);
        }
        if (isNotRoot) {
            component.subscribe("border-width", this::updateLS);
            component.subscribe("border-color", this::updateLS);
            updateLS(null);
            listeners = new EventListener[] {
                    component.addEventListener(RecalculateEvent.class, e -> updateLS(null)),
                    component.addEventListener(RepaintEvent.class, e -> invalidate()),
                    component.addEventListener(FocusEvent.class, e -> {
                        if (e.value)
                            requestFocus();
                    }),
            };
        } else
            listeners = new EventListener[0];

        if (component instanceof Container) {
            component.addDirectEventListener(AddComponentEvent.class, e -> addView(new AndroidView(e.child, context, true)));
            for (final Component c : (Container) component)
                addView(new AndroidView(c, context, true));
        }
    }

    private void updateLS(final StyleSetting ignored) {
        final int bw = component.getColor("border-color").alpha > 0 ? Math.round(Math.max(component.calcStyleNumber("border-width", Orientation.HORIZONTAL, 0), 0)) : 0;
        layout(component.startX.calcInt() - bw, component.startY.calcInt() - bw, component.endX.calcInt() + bw * 2, component.endY.calcInt() + bw * 2);
        offset = bw;
    }

    protected final AndroidGContext context;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(offset, offset);
        context.canvas = canvas;
        context.paint.setTextSize(19f * component.densityMultiplier.calcFloat() * getResources().getConfiguration().fontScale);
        component.paint(context);
    }
}