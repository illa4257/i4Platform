package i4Framework.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import i4Framework.base.components.Component;

public class AndroidView extends View {
    public final Component component;

    public AndroidView(final Component component, Context context) {
        super(context);
        this.component = component;
        final ViewGroup.MarginLayoutParams l = new ViewGroup.MarginLayoutParams(component.width.calcInt(), component.height.calcInt());
        l.leftMargin = component.startX.calcInt();
        l.topMargin = component.startY.calcInt();
        setLayoutParams(l);
        component.startX.subscribe(() -> {
            l.leftMargin = component.startX.calcInt();
            setLayoutParams(l);
        });
        component.startY.subscribe(() -> {
            l.topMargin = component.startY.calcInt();
            setLayoutParams(l);
        });
        component.width.subscribe(() -> {
            l.width = component.width.calcInt();
            setLayoutParams(l);
        });
        component.height.subscribe(() -> {
            l.height = component.height.calcInt();
            setLayoutParams(l);
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final Paint p = new Paint();
        p.setColor(Color.BLUE);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, component.width.calcFloat(), component.height.calcFloat(), p);
    }
}