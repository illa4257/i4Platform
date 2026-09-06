package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Container;
import illa4257.i4Framework.base.events.EventListener;
import illa4257.i4Framework.base.events.components.AddComponentEvent;
import illa4257.i4Framework.base.events.components.FocusEvent;
import illa4257.i4Framework.base.events.components.RecalculateEvent;
import illa4257.i4Framework.base.events.components.RepaintEvent;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.annotations.NotNull;

public class AndroidView extends ViewGroup {
    public final Component component;
    public Activity activity;

    private final boolean isNotRoot;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final EventListener<?>[] listeners;

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {}

    public static AndroidView find(final ViewGroup parent, final Component component) {
        final int c = parent.getChildCount();
        for (int i = 0; i < c; i++) {
            final View view = parent.getChildAt(i);
            if (!(view instanceof AndroidView))
                continue;
            AndroidView v = (AndroidView) view;
            if (v.component == component)
                return v;
            v = find(v, component);
            if (v != null)
                return v;
        }
        return null;
    }

    public AndroidView(final Component component, Context context, final boolean isNotRoot) {
        super(context);
        this.isNotRoot = isNotRoot;
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
            updateLS(null);
            listeners = new EventListener[] {
                    component.addEventListener(RecalculateEvent.class, e -> updateLS(null)),
                    component.addEventListener(RepaintEvent.class, e -> invalidate()),
                    component.addEventListener(FocusEvent.class, e -> {
                        if (e.value)
                            requestFocus();
                    }),
            };
        } else {
            listeners = new EventListener[]{
                    component.addEventListener(RepaintEvent.class, e -> invalidate())
            };
            component.subscribe(Component.backgroundColorProperties, p -> {
                final PropIter pi = component.getPI();
                pi.select(p, StyleProperty.paintFilter).nextLayer().nextSet();
                final Paint bg = pi.paint(Color.TRANSPARENT);
                final Window w = getAndroidWindow();
                if (w != null)
                    w.setBackgroundDrawable(new ColorDrawable((bg instanceof Color ? (Color) bg : Color.TRANSPARENT).toARGB()));
            });
        }

        if (component instanceof Container) {
            component.addDirectEventListener(AddComponentEvent.class, e -> addView(new AndroidView(e.child, context, true)));
            for (final Component c : (Container) component)
                addView(new AndroidView(c, context, true));
        }
    }

    public Window getAndroidWindow() {
        final Activity a = activity;
        if (a != null)
            return a.getWindow();
        return null;
    }

    public void updateLS(final StyleProperty ignored) {
        layout(component.renderStartX.calcInt(), component.renderStartY.calcInt(), component.renderEndX.calcInt(), component.renderEndY.calcInt());
    }

    protected final AndroidGContext context;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isNotRoot)
            setMeasuredDimension(component.width.calcInt(), component.height.calcInt());
        else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(@NotNull final Canvas canvas) {
        super.onDraw(canvas);
        context.canvas = canvas;
        context.paint.setTextSize(12f * component.dp.calcFloat() * getResources().getConfiguration().fontScale);
        component.paint(context);
    }
}