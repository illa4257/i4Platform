package illa4257.i4Utils.logger;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ErrorCollector extends LogHandler {
    private static final ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (level != Level.ERROR)
            return;
        errors.add(prefix + ": " + message);
    }

    @Override
    public void log(Level level, String prefix, String message, StackTraceElement[] stackTraceElements) {
        if (level != Level.ERROR)
            return;
        super.log(level, prefix, message, stackTraceElements);
    }

    public boolean isEmpty() { return errors.isEmpty(); }
    public String[] getErrors() { return errors.toArray(new String[0]); }
    public void clear() { errors.clear(); }
}