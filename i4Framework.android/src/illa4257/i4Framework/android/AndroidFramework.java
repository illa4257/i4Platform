package illa4257.i4Framework.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.view.KeyEvent;
import illa4257.i4Framework.android.bluetooth.AndroidBluetooth;
import illa4257.i4Framework.base.*;
import illa4257.i4Framework.base.components.Component;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.Event;
import illa4257.i4Framework.base.events.components.ChangePointEvent;
import illa4257.i4Framework.base.events.keyboard.KeyMapper;
import illa4257.i4Framework.base.capabilities.Bluetooth;
import illa4257.i4Utils.media.Color;
import illa4257.i4Utils.media.Image;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.web.i4URI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class AndroidFramework extends Framework {
    public static final float SCALE_FACTOR = 1.5f;

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

    @Override
    public PopupMenu newPopupMenu(final Component component) {
        return new PopupMenu() {
            public final android.widget.PopupMenu menu = new android.widget.PopupMenu(
                    context,
                    AndroidView.find(((AndroidWindow) component.getWindow().frameworkWindow.get()).root, component)
            );

            @Override
            public PopupMenu add(final String text, final Runnable action) {
                menu.getMenu().add(text).setOnMenuItemClickListener(item -> {
                    action.run();
                    return true;
                });
                return this;
            }

            @Override
            public PopupMenu show() {
                menu.show();
                return this;
            }
        };
    }

    @Override
    public Dialog newDialog(final Window window) {
        return new Dialog() {
            final AlertDialog.Builder builder = new AlertDialog.Builder(((AndroidWindow) window.frameworkWindow.get()).activity.get());

            @Override
            public Dialog setTitle(final String title) {
                builder.setTitle(title);
                return this;
            }

            @Override
            public Dialog setMessage(final String message) {
                builder.setMessage(message);
                return this;
            }

            @Override
            public Dialog setContent(Component component) { /// TODO: Fix
                final Window w = new Window();
                w.addDirectEventListener(ChangePointEvent.class, e -> {
                    new Throwable().printStackTrace();
                    System.out.println(w.endX.get());
                });
                final AndroidWindow aw = new AndroidWindow(AndroidFramework.this, w);
                w.frameworkWindow.set(aw);
                w.link();
                windows.offer(aw);
                component = new Component() {
                    @Override
                    public void paint(illa4257.i4Framework.base.Context context) {
                        super.paint(context);
                        context.setColor(Color.RED);
                        context.drawRect(0, 0, 256, 256);
                    }
                };

                w.add(component);
                aw.window.setSize(128, 128);
                component.setSize(128, 128);
                aw.root.updateLS(null);
                aw.root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    aw.updateSafeZone(aw.root.getRootWindowInsets());
                    w.setSize(aw.root.getWidth(), aw.root.getHeight(), true);
                });
                final Component c = component;
                new Thread(() -> {
                    try {
                        while (true) {
                            Thread.sleep(1000);
                            System.out.println(aw.root.getWidth() + " x " + aw.root.getHeight() + " / " + w.width.calcInt() + " x " + w.height.calcInt() + " / " + c.width.calcInt() + " x " + c.height.calcInt());
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                builder.setView(aw.root);
                return this;
            }

            @Override
            public Dialog setPositiveButton(final String name, final Runnable action) {
                builder.setPositiveButton(name, (d, which) -> action.run());
                return this;
            }

            @Override
            public Dialog setNegativeButton(final String name, final Runnable action) {
                builder.setNegativeButton(name, (d, which) -> action.run());
                return this;
            }

            @Override
            public Dialog setOnCancelListener(final Runnable action) {
                builder.setOnCancelListener(dialog -> action.run());
                return this;
            }

            @Override
            public Dialog show() {
                builder.show();
                return this;
            }
        };
    }

    //* Not finished
    @Override
    public FileChooser newFileChooser() {
        return new AndroidFileChooser(this);
    }

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

    @Override public File getAppDataDir() { return context.getFilesDir(); }

    @Override public File getLocalAppDataDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                context.getNoBackupFilesDir() :
                context.getFilesDir();
    }

    /// TODO: Change it.
    @Override public File getAppDir() { return new File(context.getApplicationInfo().sourceDir).getParentFile(); }

    public static Activity getActivity(final Window window) {
        return ((AndroidWindow) window.frameworkWindow.get()).activity.get();
    }

    @Override
    public CompletableFuture<Bluetooth> getBluetooth(final Window window) {
        return CompletableFuture.completedFuture(new AndroidBluetooth(getActivity(window)));
    }

    /*@Override
    public <T extends Capability> CompletableFuture<T> request(final Window window, final Class<T> permission) {
        if (permission == BluetoothAdvertise.class) {
            final AndroidWindow aw = (AndroidWindow) window.frameworkWindow.get();
            final Activity a = aw.activity.get();
            final int code = 0;
            final CompletableFuture<T> future = new CompletableFuture<>();
            aw.requests.put(code, b -> {
                //noinspection unchecked
                future.complete(b ? (T) new AndroidBluetooth(context.getSystemService(BluetoothManager.class)) : null);
            });
            a.requestPermissions(new String[]{ Manifest.permission.BLUETOOTH_ADVERTISE }, code);
            return future;
        }
        return super.request(window, permission);
    }*/
}