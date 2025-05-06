package illa4257.i4Utils.logger;

import java.io.PrintStream;

public class AnsiColoredPrintStreamLogHandler extends PrintStreamLogHandler implements ILogHandler {
    public static final String
            ANSI_RESET = "\u001B[0m",
            ANSI_RED = "\u001B[31m",
            ANSI_YELLOW = "\u001B[33m",
            ANSI_BLUE = "\u001B[34m",
            ANSI_WHITE = "\u001B[37m",
            ANSI_BRIGHT_BLACK = "\u001B[90m";

    public AnsiColoredPrintStreamLogHandler(final PrintStream stream) { super(stream); }

    public static String formatMessage(final Level level, final String message) {
        return (level == Level.ERROR ? ANSI_RED :
                level == Level.WARN ? ANSI_YELLOW :
                level == Level.DEBUG ? ANSI_BRIGHT_BLACK :
                                        ANSI_WHITE) + message;
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        stream.println(ANSI_BLUE + format(level, prefix) + ANSI_WHITE + ": " + formatMessage(level, message + ANSI_RESET));
    }

    @Override
    public void log(Level level, final String prefix, String message, StackTraceElement[] stackTraceElements) {
        final StringBuilder b = stringBuilder.get();
        b.setLength(0);
        b.append(ANSI_BLUE).append(format(level, prefix)).append(ANSI_WHITE).append(": ").append(
                level == Level.ERROR ? ANSI_RED :
                level == Level.WARN ? ANSI_YELLOW :
                        level == Level.DEBUG ? ANSI_BRIGHT_BLACK : ANSI_WHITE).append(message);
        for (final StackTraceElement element : stackTraceElements)
            b.append(System.lineSeparator()).append("\tat ").append(element);
        b.append(ANSI_RESET);
        stream.println(b);
    }
}