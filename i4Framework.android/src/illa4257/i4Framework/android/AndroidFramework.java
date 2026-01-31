package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.view.KeyEvent;
import illa4257.i4Framework.base.Framework;
import illa4257.i4Framework.base.FrameworkWindow;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.keyboard.KeyMapper;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.web.i4URI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class AndroidFramework extends Framework {
    static final ConcurrentLinkedQueue<Activity> activities = new ConcurrentLinkedQueue<>();
    private static int processing = 0;

    public static final KeyMapper KEY_MAP = new KeyMapper()
            .del(KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL)
            .helpers(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_ALT_LEFT)
            .arrows(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)
            .functional(KeyEvent.KEYCODE_COPY, KeyEvent.KEYCODE_PASTE, KeyEvent.KEYCODE_MENU);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final boolean isDark = context.getResources().getConfiguration().isNightModeActive();
            onSystemThemeChange(isDark ? "dark" : "light", isDark ? BaseTheme.DARK : BaseTheme.LIGHT);
        }
    }

    public static void pass(final AndroidActivity activity) {
        activities.add(activity);
        synchronized (activities) {
            if (processing > 0)
                processing--;
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
    public void fireAllWindows(final Function<Window, Event> event) {
        for (final AndroidWindow window : windows)
            window.window.fire(event.apply(window.window));
    }

    @Override
    public boolean isUIThread(Component component) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? context.getMainLooper().isCurrentThread()
                : Thread.currentThread() == context.getMainLooper().getThread();
    }

    @Override protected void onSystemThemeChange(final String theme, final BaseTheme baseTheme) { super.onSystemThemeChange(theme, baseTheme); }

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

    /* Not finished
    @Override
    public IFileChooser newFileChooser() {
        return new AndroidFileChooser(this);
    }*/

    @Override
    public InputStream openResource(final i4URI uri) {
        if (uri.fullPath != null) {
            final String s = uri.fullPath.startsWith("/") ? uri.fullPath.substring(1) :
                    uri.fullPath;
            try {
                return context.getAssets().open(s.replaceAll("/", "\\\\"));
            } catch (final Exception ignored) {
                try {
                    return context.getAssets().open(s.replaceAll("\\\\", "/"));
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(ex);
                }
            }
        }
        return super.openResource(uri);
    }

    @Override
    public Image getImage(final InputStream inputStream) throws IOException {
        try {
            final Bitmap b = BitmapFactory.decodeStream(inputStream);
            return new Image(b.getWidth(), b.getHeight(), AndroidImage.class, new AndroidImage(b));
        } catch (final Exception ignored) {
            return super.getImage(inputStream);
        }
    }

    @Override public File getAppDataDir() { return new File(context.getApplicationInfo().dataDir); }
    @Override public File getLocalAppDataDir() {return context.getFilesDir(); }
    @Override public File getAppDir() { return new File(context.getApplicationInfo().sourceDir).getParentFile(); }
}