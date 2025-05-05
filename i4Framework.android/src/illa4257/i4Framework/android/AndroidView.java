package illa4257.i4Framework.android;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;

public class AndroidView extends ViewGroup {
    public final Component component;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final EventListener<?>[] listeners;

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {}

    public AndroidView(final Component component, Context context, final boolean isNotRoot) {
        super(context);
        setWillNotDraw(false);
        this.component = component;
        this.context = new AndroidGContext();
        {
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(component.width.calcInt(), component.height.calcInt());
            lp.leftMargin = component.startX.calcInt();
            lp.topMargin = component.startY.calcInt();
            setLayoutParams(lp);
        }
        if (isNotRoot) {
            layout(component.startX.calcInt(), component.startY.calcInt(), component.endX.calcInt(), component.endY.calcInt());
            listeners = new EventListener[] {
                    component.addEventListener(RecalculateEvent.class, e ->
                            layout(component.startX.calcInt(), component.startY.calcInt(), component.endX.calcInt(), component.endY.calcInt())),
                    component.addEventListener(RepaintEvent.class, e -> invalidate())
            };
        } else
            listeners = new EventListener[0];

        if (component instanceof Container) {
            component.addDirectEventListener(AddComponentEvent.class, e -> addView(new AndroidView(e.child, context, true)));
            for (final Component c : (Container) component)
                addView(new AndroidView(c, context, true));
        }
    }

    protected final AndroidGContext context;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        context.canvas = canvas;
        context.paint.setTextSize(19f * component.densityMultiplier.calcFloat() * getResources().getConfiguration().fontScale);
        component.paint(context);
    }
}