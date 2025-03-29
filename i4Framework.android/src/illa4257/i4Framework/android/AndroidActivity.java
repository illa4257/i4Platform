package illa4257.i4Framework.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
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
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        final AndroidWindow w = frameworkWindow.get();
        return w != null ? w.onDispatch(ev) : super.dispatchTouchEvent(ev);
    }
}