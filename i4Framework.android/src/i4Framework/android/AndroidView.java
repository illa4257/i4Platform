package i4Framework.android;

import android.content.Context;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import i4Framework.base.components.Component;
import i4Framework.base.components.Container;
import i4Framework.base.events.components.AddComponentEvent;
import i4Framework.base.events.components.RecalculateEvent;

public class AndroidView extends ViewGroup {
    public final Component component;

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {}

    public AndroidView(final Component component, Context context) {
        super(context);
        setWillNotDraw(false);
        this.component = component;
        {
            final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(component.width.calcInt(), component.height.calcInt());
            lp.leftMargin = component.startX.calcInt();
            lp.topMargin = component.startY.calcInt();
            setLayoutParams(lp);
        }
        layout(component.startX.calcInt(), component.startY.calcInt(), component.endX.calcInt(), component.endY.calcInt());
        component.addEventListener(RecalculateEvent.class, e -> {
            layout(component.startX.calcInt(), component.startY.calcInt(), component.endX.calcInt(), component.endY.calcInt());
        });
        if (component instanceof Container) {
            component.addDirectEventListener(AddComponentEvent.class, e -> {
                addView(new AndroidView(e.child, context));
            });
            for (final Component c : (Container) component)
                addView(new AndroidView(c, context));
        }
    }

    protected final AndroidGContext context = new AndroidGContext();

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        context.canvas = canvas;
        component.paint(context);
    }
}