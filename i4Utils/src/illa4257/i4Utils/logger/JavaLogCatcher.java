package illa4257.i4Utils.logger;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static illa4257.i4Utils.logger.Level.*;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

public class JavaLogCatcher extends Handler {
    public static illa4257.i4Utils.logger.Level getLevel(final Level level) {
        if (
                level == FINE ||
                        level == FINER ||
                        level == FINEST
        )
            return DEBUG;
        else if (level == WARNING)
            return WARN;
        else if (level == SEVERE)
            return ERROR;
        return INFO;
    }

    public final ILogHandler logger;

    public JavaLogCatcher(final ILogHandler logger) { this.logger = logger; }

    @Override
    public void publish(final LogRecord logRecord) {
        final String msg = logRecord.getMessage();
        final Throwable throwable = logRecord.getThrown();

        if (
                msg != null && !msg.isEmpty() &&
                        logRecord.getThrown() != null
        )
            logger.log(getLevel(logRecord.getLevel()), logRecord.getLoggerName(),
                    msg + ' ' + throwable, throwable.getStackTrace());
        else if (msg != null && !msg.isEmpty())
            logger.log(getLevel(logRecord.getLevel()), logRecord.getLoggerName(), msg);
        else if (logRecord.getThrown() != null)
            logger.log(getLevel(logRecord.getLevel()), logRecord.getLoggerName(), throwable);
    }

    @Override public void flush() {}
    @Override public void close() {}
}