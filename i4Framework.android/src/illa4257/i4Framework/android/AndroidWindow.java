package illa4257.i4Framework.android;

import android.app.Activity;
import android.view.MotionEvent;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import illa4257.i4Framework.base.events.mouse.*;
import illa4257.i4Framework.base.events.touchscreen.TouchDownEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchMoveEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.logger.i4Logger;

public class AndroidWindow implements FrameworkWindow {
    public final AndroidFramework framework;
    public final SyncVar<Activity> activity = new SyncVar<>();
    public final Window window;
    public final AndroidView root;

    public AndroidWindow(final AndroidFramework framework) {
        this.framework = framework;
        window = new Window();
        root = new AndroidView(window, framework.context, false);
        init();
    }

    public AndroidWindow(final AndroidFramework framework, final Window window) {
        this.framework = framework;
        this.window = window != null ? window : new Window();
        root = new AndroidView(this.window, framework.context, false);
        init();
    }

    private void init() {
        window.addDirectEventListener(VisibleEvent.class, e -> {
            if (e.value) {
                if (!window.frameworkWindow.setIfNull(this))
                    return;
                window.link();
                framework.windows.add(this);
                new Thread(() -> {
                    try {
                        final Activity a = framework.getActivity();
                        if (a instanceof AndroidActivity)
                            ((AndroidActivity) a).frameworkWindow.set(this);
                        framework.invokeLater(() -> {
                            activity.set(a);
                            a.setContentView(root);
                            window.setSize(root.getWidth(), root.getHeight());
                            root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                                window.endX.set(new PointAttach(root.getWidth(), null));
                                window.endY.set(new PointAttach(root.getHeight(), null));
                                window.fire(new ChangePointEvent());
                                window.repaint();
                            });
                        });
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                }).start();
            } else
                dispose();
        });
    }

    @Override public Framework getFramework() { return framework; }
    @Override public Window getWindow() { return window; }

    public boolean onDispatch(final MotionEvent e) {
        final int number = e.getPointerCount();
        final float[] pos = new float[2];
        for (int i = 0; i < number; i++) {
            final float gx = e.getX(i), gy = e.getY(i);
            final Component c = window.find(gx, gy, pos);
            if (c == null)
                continue;
            switch (e.getToolType(i)) {
                case MotionEvent.TOOL_TYPE_FINGER:
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            c.fire(new TouchMoveEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i)));
                            break;
                        case MotionEvent.ACTION_DOWN:
                            c.fire(new TouchDownEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i)));
                            break;
                        case MotionEvent.ACTION_UP:
                            c.fire(new TouchUpEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i)));
                            break;
                        default:
                            System.out.println("Unknown action: " + e.getAction());
                            break;
                    }
                    break;
                case MotionEvent.TOOL_TYPE_MOUSE:
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            c.fire(new MouseMoveEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i)));
                            break;
                        case MotionEvent.ACTION_DOWN:
                            c.fire(new MouseDownEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i), getMouseButton(e.getButtonState())));
                            break;
                        case MotionEvent.ACTION_UP:
                            c.fire(new MouseUpEvent(gx, gy, pos[0], pos[1], true, e.getPointerId(i), getMouseButton(e.getButtonState())));
                            break;
                        default:
                            System.out.println("Unknown action: " + e.getAction());
                            break;
                    }
                    break;
                default:
                    System.out.println("Unknown tool type " + e.getToolType(i));
                    break;
            }
        }
        return true;
    }

    public static MouseButton getMouseButton(final int btn) {
        switch (btn) {
            case MotionEvent.BUTTON_PRIMARY:
                return MouseButton.BUTTON0;
            case MotionEvent.BUTTON_SECONDARY:
                return MouseButton.BUTTON1;
            case MotionEvent.BUTTON_TERTIARY:
                return MouseButton.BUTTON2;
            default:
                System.out.println("Unknown button: " + btn);
                return MouseButton.UNKNOWN_BUTTON;
        }
    }

    @Override
    public void dispose() {
        final Activity a = activity.getAndSet(null);
        if (a == null)
            return;
        framework.windows.remove(this);
        a.finish();
    }
}