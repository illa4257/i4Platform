package i4Utils.logger;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ErrorCollector implements LogHandler {
    private static final String ln = System.lineSeparator();
    private static final ConcurrentLinkedQueue<String> errors = new ConcurrentLinkedQueue<>();

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (level != Level.ERROR)
            return;
        errors.add(prefix + ": " + message);
    }

    @Override
    public void log(Level level, final String prefix, String message, StackTraceElement[] stackTraceElements) {
        if (level != Level.ERROR)
            return;
        final StringBuilder b = new StringBuilder(prefix).append(": ").append(message);
        for (final StackTraceElement e : stackTraceElements)
            b.append(ln).append("\tat ").append(e);
        errors.add(b.toString());
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    public String[] getErrors() {
        return errors.toArray(new String[0]);
    }

    public void clear() {
        errors.clear();
    }
}