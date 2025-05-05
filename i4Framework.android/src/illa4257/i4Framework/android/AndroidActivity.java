package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import illa4257.i4Framework.base.styling.BaseTheme;
import illa4257.i4Utils.SyncVar;

public class AndroidActivity extends Activity {
    public final SyncVar<AndroidWindow> frameworkWindow = new SyncVar<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        AndroidFramework.pass(this);
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        final AndroidWindow w = frameworkWindow.get();
        if (w != null) {
            w.densityMultiplier.set(newConfig.densityDpi / 160.0f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                w.framework.onSystemThemeChange(
                        newConfig.isNightModeActive() ? "dark" : "light",
                        newConfig.isNightModeActive() ? BaseTheme.LIGHT : BaseTheme.DARK
                );
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        final AndroidWindow w = frameworkWindow.get();
        return w != null ? w.onDispatch(ev) : super.dispatchTouchEvent(ev);
    }
}