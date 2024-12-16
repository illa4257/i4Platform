package i4Framework.android;

import android.content.Intent;
import android.view.ViewTreeObserver;
import i4Framework.base.Framework;
import i4Framework.base.FrameworkWindow;
import i4Framework.base.components.Window;
import i4Framework.base.events.VisibleEvent;
import i4Framework.base.events.components.AddComponentEvent;
import i4Framework.base.points.PointAttach;

import java.util.concurrent.ConcurrentHashMap;

public class AndroidWindow implements FrameworkWindow {
    static final ConcurrentHashMap<Integer, AndroidFramework> transferFrameworks = new ConcurrentHashMap<>();

    public final AndroidFramework framework;
    public final Window window;
    AndroidActivity activity = null;

    public AndroidWindow(final AndroidFramework framework, Window window) {
        if (framework == null)
            throw new IllegalArgumentException("Framework cannot be null");
        this.framework = framework;
        final Window w = this.window = window == null ? new Window() : window;
        w.addDirectEventListener(VisibleEvent.class, e -> {
            if (!e.value) {
                synchronized (w.getLocker()) {
                    if (w.isVisible())
                        return;
                    if (this.activity != null) {
                        this.activity.finish();
                        this.activity = null;
                    }
                    framework.windows.remove(this);
                    framework.windowsUpdated();
                }
                return;
            }
            final int id = hashCode();
            transferFrameworks.put(id, framework);
            synchronized (framework.windowLocker) {
                new Thread(() -> {
                    synchronized (framework.windowLocker) {
                        try {
                            while (framework.result == null) {
                                if (!framework.processing) {
                                    Intent intent = new Intent(framework.context, AndroidActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                    intent.putExtra("framework", id);
                                    framework.context.startActivity(intent);
                                    framework.processing = true;
                                }
                                framework.windowLocker.wait();
                            }
                            synchronized (w.getLocker()) {
                                if (w.isVisible()) {
                                    final AndroidActivity a = this.activity = framework.result;
                                    a.root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            w.setSize(a.root.getWidth(), a.root.getHeight());
                                        }
                                    });
                                }
                            }
                            framework.result = null;
                            framework.processing = false;
                            framework.windowLocker.notifyAll();
                            framework.windows.add(this);
                            framework.windowsUpdated();
                        } catch (final InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }).start();
            }
        });
        w.addEventListener(AddComponentEvent.class, e -> {
            synchronized (w.getLocker()) {
                if (activity == null)
                    return;
                final AndroidActivity a = activity;
                framework.uiHandler.post(() -> a.root.addView(new AndroidView(e.child, framework.context)));
            }
        });
    }

    @Override
    public Framework getFramework() {
        return framework;
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public void dispose() {
        window.setVisible(false);
    }
}
