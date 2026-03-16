package illa4257.i4Utils.logger;

import java.io.PrintStream;

public class RawPrintStreamLogHandler extends LogHandler {
    public final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    protected final PrintStream stream;

    public RawPrintStreamLogHandler(final PrintStream stream) { this.stream = stream; }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        stream.println(message);
    }

    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        final StringBuilder b = stringBuilder.get();
        b.setLength(0);
        b.append(message);
        for (final StackTraceElement element : stackTraceElements)
            b.append(System.lineSeparator()).append("\tat ").append(element);
        stream.println(b);
    }
}