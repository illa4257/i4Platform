package i4Framework.android;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;
import i4Framework.base.Framework;
import i4Framework.base.FrameworkWindow;
import i4Framework.base.components.Component;
import i4Framework.base.components.Window;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AndroidFramework extends Framework {
    public static final Object locker = new Object();
    public static AndroidFramework INSTANCE = null;
    public final Context context;
    public final Handler uiHandler;

    final Object windowLocker = new Object();
    boolean processing = false;
    AndroidActivity result = null;

    public AndroidFramework(final Context context) {
        this.context = context;
        uiHandler = new Handler(context.getMainLooper());
        synchronized (locker) {
            if (INSTANCE == null)
                INSTANCE = this;
        }
    }

    final ConcurrentLinkedQueue<AndroidWindow> windows = new ConcurrentLinkedQueue<>();
    Choreographer.FrameCallback callback = null;
    void windowsUpdated() {
        if (!isUIThread(null)) {
            uiHandler.post(this::windowsUpdated);
            return;
        }
        synchronized (windows) {
            final boolean isEmpty = windows.isEmpty();
            if (isEmpty && callback != null)
                callback = null;
            else if (!isEmpty && callback == null) {
                Choreographer.getInstance().postFrameCallback(callback = new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNanos) {
                        for (final AndroidWindow window : windows)
                            window.window.invokeAll();
                        synchronized (windows) {
                            if (callback == this)
                                Choreographer.getInstance().postFrameCallback(this);
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean isUIThread(Component component) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? context.getMainLooper().isCurrentThread()
                : Thread.currentThread() == context.getMainLooper().getThread();
    }

    @Override
    public FrameworkWindow newWindow(Window window) {
        return new AndroidWindow(this, window);
    }
}