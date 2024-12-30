package illa4257.i4Framework.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class AndroidActivity extends Activity {
    public LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        root = new LinearLayout(getApplicationContext());
        root.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        final AndroidFramework framework = AndroidWindow.transferFrameworks.remove(getIntent().getIntExtra("framework", -1));

        synchronized(framework.windowLocker) {
            try {
                while (framework.result != null)
                    framework.windowLocker.wait();
            } catch (final InterruptedException ex) {
                Intent intent = new Intent(framework.context, AndroidActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                framework.context.startActivity(intent);
                throw new RuntimeException(ex);
            }
            framework.result = this;
            framework.windowLocker.notifyAll();
        }
    }


}