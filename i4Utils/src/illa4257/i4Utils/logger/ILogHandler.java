package illa4257.i4Utils.logger;

public interface ILogHandler {
    void log(final Level level, final String prefix, final String message);
    void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements);
    void log(final Level level, final String prefix, final Throwable throwable);
    void log(final Level level, final String prefix, final Object... objects);
    void log(final Level level, final String prefix, final Object object);

    default void log(final String prefix, final Throwable throwable) {
        log(Level.ERROR, prefix, throwable);
    }
}