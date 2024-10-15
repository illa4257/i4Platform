package i4Utils.logger;

public interface LogHandler {
    void log(final Level level, final String prefix, final String message);
    void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements);
}