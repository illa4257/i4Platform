package illa4257.i4Utils.logger;

import java.io.PrintStream;

public class PrintStreamLogHandler implements LogHandler {
    protected final Object locker;
    public final Level level;
    protected final PrintStream stream;

    public PrintStreamLogHandler(final PrintStream stream, final Level level, final Object locker) {
        if (stream == null)
            throw new NullPointerException();
        this.locker = locker == null ? new Object() : locker;
        this.stream = stream;
        this.level = level;
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (this.level != null && this.level != level)
            return;
        synchronized (locker) {
            stream.println(prefix + ": " + message);
        }
    }

    @Override
    public void log(Level level, final String prefix, String message, StackTraceElement[] stackTraceElements) {
        if (this.level != null && this.level != level)
            return;
        synchronized (locker) {
            stream.println(prefix + ": " + message);
            for (final StackTraceElement element : stackTraceElements)
                stream.println("\tat " + element);
        }
    }
}