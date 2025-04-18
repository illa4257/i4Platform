package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;

import java.util.concurrent.ConcurrentLinkedQueue;

public class AndroidFramework extends Framework {
    static final ConcurrentLinkedQueue<Activity> activities = new ConcurrentLinkedQueue<>();
    private static int processing = 0;

    private boolean isNotEnabled = true;
    final ConcurrentLinkedQueue<AndroidWindow> windows = new ConcurrentLinkedQueue<AndroidWindow>() {
        @Override
        public boolean add(AndroidWindow androidWindow) {
            if (super.add(androidWindow)) {
                synchronized (windows) {
                    if (isNotEnabled)
                        onEnable();
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (super.remove(o)) {
                synchronized (windows) {
                    if (!isNotEnabled)
                        onDisable();
                }
                return true;
            }
            return false;
        }
    };

    public final Context context;
    public final Handler uiHandler;
    private Runnable callback;

    public AndroidFramework(final Context context) {
        this.context = context;
        uiHandler = new Handler(context.getMainLooper());
        callback = () -> {
            for (final AndroidWindow window : windows)
                window.window.invokeAll();
            uiHandler.postDelayed(callback, 16);
        };
    }

    public static void pass(final AndroidActivity activity) {
        activities.add(activity);
        synchronized (activities) {
            activities.notify();
        }
    }

    public Activity getActivity() throws InterruptedException {
        Activity r = activities.poll();
        if (r != null)
            return r;
        synchronized (activities) {
            while ((r = activities.poll()) == null) {
                if (processing == 0)
                    launchActivity();
                activities.wait();
            }
            return r;
        }
    }

    @Override
    public void fireAllWindows(final Event event) {
        for (final AndroidWindow window : windows)
            window.window.fire(event);
    }

    @Override
    public boolean isUIThread(Component component) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? context.getMainLooper().isCurrentThread()
                : Thread.currentThread() == context.getMainLooper().getThread();
    }

    void onEnable() {
        isNotEnabled = false;
        uiHandler.postDelayed(callback, 16);
    }

    void onDisable() {
        isNotEnabled = true;
        uiHandler.removeCallbacks(callback);
    }

    void launchActivity() {
        synchronized (activities) {
            if (!isUIThread(null)) {
                uiHandler.post(this::launchActivity);
                return;
            }
            processing++;
            final Intent i = new Intent(context, AndroidActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            context.startActivity(i);
        }
    }

    @Override public void invokeLater(final Runnable runnable) { uiHandler.post(runnable); }
    @Override public FrameworkWindow newWindow(final Window window) { return new AndroidWindow(this, window); }
}