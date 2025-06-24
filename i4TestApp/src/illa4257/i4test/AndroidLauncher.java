package illa4257.i4test;

import android.os.Bundle;
import illa4257.i4Framework.android.AndroidActivity;
import illa4257.i4Framework.android.AndroidFramework;
import illa4257.i4Utils.logger.AndroidLogger;

public class AndroidLauncher extends AndroidActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        i4Test.L.registerHandler(new AndroidLogger());
        i4Test.init(new AndroidFramework(getBaseContext()));
    }
}