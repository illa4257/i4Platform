package illa4257.i4Framework.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.view.*;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.events.components.VisibleEvent;
import illa4257.i4Framework.base.events.mouse.*;
import illa4257.i4Framework.base.events.touchscreen.TouchDownEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchMoveEvent;
import illa4257.i4Framework.base.events.touchscreen.TouchUpEvent;
import illa4257.i4Framework.base.points.Point;
import illa4257.i4Framework.base.points.PointAttach;
import illa4257.i4Framework.base.points.numbers.NumberPoint;
import illa4257.i4Framework.base.points.numbers.NumberPointConstant;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Utils.SyncVar;
import illa4257.i4Utils.logger.i4Logger;

public class AndroidWindow implements FrameworkWindow {
    public final AndroidFramework framework;
    public final SyncVar<Activity> activity = new SyncVar<>();
    public final Window window;
    public final AndroidView root;
    public final NumberPoint densityMultiplier = new NumberPoint();

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

    private void updateSafeZone(final WindowInsets insets) {
        final Insets rect = insets.getInsets(
                WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout()
        );

        window.safeStartX.set(new NumberPointConstant(rect.left));
        window.safeStartY.set(new NumberPointConstant(rect.top));
        window.safeEndX.set(new NumberPointConstant(root.getWidth() - rect.right));
        window.safeEndY.set(new NumberPointConstant(root.getHeight() - rect.bottom));
    }

    private volatile boolean isDark = false;

    protected void onThemeUpdate(final String theme, final BaseTheme baseTheme) {
        final Activity a = activity.get();
        if (a == null)
            return;
        final View d = a.getWindow().getDecorView();
        d.post(() -> {
            isDark = baseTheme == BaseTheme.DARK;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController controller = a.getWindow().getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(
                            isDark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,

                            WindowInsetsController.APPEARANCE_LIGHT_CAPTION_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS |
                                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    );
                }
            } else {
                //noinspection deprecation
                a.getWindow().getDecorView().setSystemUiVisibility(
                        isDark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            }
        });
    }

    private void init() {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            updateSafeZone(insets);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
                view.requestApplyInsets();
            else
                //noinspection deprecation
                view.requestFitSystemWindows();
            return WindowInsets.CONSUMED;
        });
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            window.endX.set(new PointAttach(root.getWidth(), null));
            window.endY.set(new PointAttach(root.getHeight(), null));
            updateSafeZone(root.getRootWindowInsets());
            window.fire(new ChangePointEvent());
            window.repaint();
        });
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
                        densityMultiplier.set(a.getResources().getDisplayMetrics().density);
                        framework.invokeLater(() -> {
                            activity.set(a);
                            a.setContentView(root);
                            window.setSize(root.getWidth(), root.getHeight());
                            window.densityMultiplier.set(densityMultiplier);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                                    //noinspection deprecation
                                    a.getWindow().setDecorFitsSystemWindows(false);
                                final WindowInsetsController controller = a.getWindow().getInsetsController();
                                if (controller != null)
                                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            } else {
                                //noinspection deprecation
                                a.getWindow().getDecorView().setSystemUiVisibility(
                                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                );
                                //noinspection deprecation
                                a.getWindow().setStatusBarColor(Color.TRANSPARENT);
                                //noinspection deprecation
                                a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
                            }
                            framework.addThemeListener(this::onThemeUpdate);
                            onThemeUpdate(framework.getTheme(), framework.getBaseTheme());
                            window.fire(new StyleUpdateEvent());
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
    public Point getDensityMultiplier() {
        return densityMultiplier;
    }

    @Override
    public void dispose() {
        final Activity a = activity.getAndSet(null);
        if (a == null)
            return;
        framework.removeThemeListener(this::onThemeUpdate);
        framework.windows.remove(this);
        a.finishAndRemoveTask();
    }
}