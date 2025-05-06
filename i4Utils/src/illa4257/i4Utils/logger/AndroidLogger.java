package illa4257.i4Utils.logger;

import android.util.Log;

public class AndroidLogger extends LogHandler {
    @Override
    public void log(final Level level, final String prefix, final String message) {
        switch (level) {
            case INFO:
                Log.i(prefix, message);
                break;
            case WARN:
                Log.w(prefix, message);
                break;
            case ERROR:
                Log.e(prefix, message);
                break;
            case DEBUG:
                Log.d(prefix, message);
                break;
        }
    }
}