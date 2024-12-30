package illa4257.i4Utils.logger;

import java.io.PrintStream;

public class AnsiColoredPrintStreamLogHandler extends PrintStreamLogHandler implements LogHandler {
    public static final String
            ANSI_RESET = "\u001B[0m",
            ANSI_RED = "\u001B[31m",
            ANSI_YELLOW = "\u001B[33m",
            ANSI_BLUE = "\u001B[34m",
            ANSI_WHITE = "\u001B[37m",
            ANSI_BRIGHT_BLACK = "\u001B[90m";

    public AnsiColoredPrintStreamLogHandler(final PrintStream stream, final Level level, final Object locker) {
        super(stream, level, locker);
    }

    private static String format(final Level level, final String message) {
        return (level == Level.ERROR ? ANSI_RED :
                level == Level.WARN ? ANSI_YELLOW :
                level == Level.DEBUG ? ANSI_BRIGHT_BLACK :
                                        ANSI_WHITE) + message;
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (this.level != null && this.level != level)
            return;
        super.log(level, ANSI_BLUE + prefix + ANSI_WHITE, format(level, message + ANSI_RESET));
    }

    @Override
    public void log(Level level, final String prefix, String message, StackTraceElement[] stackTraceElements) {
        if (this.level != null && this.level != level)
            return;
        synchronized (locker) {
            stream.println(ANSI_BLUE + prefix + ANSI_WHITE + ": " + format(level, message));
            for (final StackTraceElement element : stackTraceElements)
                stream.println("\tat " + element);
            stream.print(ANSI_RESET);
        }
    }
}