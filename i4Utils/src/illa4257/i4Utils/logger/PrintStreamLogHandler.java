package illa4257.i4Utils.logger;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PrintStreamLogHandler extends LogHandler {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String format(final Level level, final String name) {
        return '[' + LocalDateTime.now().format(TIME_FORMATTER) + "][" + level + ']' + (name != null ? '[' + name + ']' : "");
    }

    public final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(StringBuilder::new);
    protected final PrintStream stream;

    public PrintStreamLogHandler(final PrintStream stream) { this.stream = stream; }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        stream.println(format(level, prefix) + ": " + message);
    }

    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        final StringBuilder b = stringBuilder.get();
        b.setLength(0);
        b.append(format(level, prefix)).append(": ").append(message);
        for (final StackTraceElement element : stackTraceElements)
            b.append(System.lineSeparator()).append("\tat ").append(element);
        stream.println(b);
    }
}