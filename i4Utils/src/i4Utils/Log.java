package i4Utils;

public class Log {
    public static void printStacktrace(final StackTraceElement[] stackTrace) {
        for (final StackTraceElement e : stackTrace)
            System.err.println(" at " + e);
    }
}