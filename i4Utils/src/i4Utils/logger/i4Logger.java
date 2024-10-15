package i4Utils.logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class i4Logger implements LogHandler {
    public final String name;
    private final ConcurrentLinkedQueue<LogHandler> handlers = new ConcurrentLinkedQueue<>();

    public i4Logger() {
        this.name = null;
    }

    public i4Logger(final String name) {
        this.name = name;
    }

    public boolean registerHandler(final LogHandler handler) {
        if (handler == null)
            return false;
        return handlers.add(handler);
    }

    public boolean unregisterHandler(final LogHandler handler) {
        if (handler == null)
            return false;
        return handlers.remove(handler);
    }

    public i4Logger sub(final String name) {
        if (name == null)
            throw new NullPointerException();
        final i4Logger r = new i4Logger(this.name == null ? name : this.name + '/' + name);
        r.registerHandler(this);
        return r;
    }

    private String prefix(final Level level) {
        return "[" + LocalDateTime.now() + "][" + level + "]" + (name == null ? "" : "[" + name + "]");
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (level == null)
            return;
        for (final LogHandler handler : handlers)
            handler.log(level, prefix, message);
    }
    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        if (level == null)
            return;
        for (final LogHandler handler : handlers)
            handler.log(level, prefix, message, stackTraceElements);
    }

    public void log(final Level level, final String message) {
        if (level == null)
            return;
        log(level, prefix(level), message);
    }

    public void log(final Level level, final String message, final StackTraceElement[] stackTraceElements) {
        if (level == null)
            return;
        log(level, prefix(level), message, stackTraceElements);
    }

    public void log(final Level level, Throwable throwable) {
        if (level == null)
            return;
        log(level, prefix(level), throwable.toString(), throwable.getStackTrace());
    }

    public void log(final Throwable throwable) {
        log(Level.ERROR, prefix(Level.ERROR), throwable.toString(), throwable.getStackTrace());
    }

    public void log(final Level level, final Object... objects) {
        if (level == null)
            return;
        log(level, prefix(level), objects == null ? "null" : Arrays.hashCode(objects) + Arrays.toString(objects));
    }

    public void log(final Level level, final Object object) {
        if (level == null)
            return;
        log(level, prefix(level), object instanceof Object[] ?
                Arrays.hashCode((Object[]) object) + Arrays.toString((Object[]) object) : Objects.toString(object));
    }

    public OutputStream newOutputStream(final Level level) {
        return new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(final int ch) {
                if (ch == '\n') {
                    log(level, buffer.toString());
                    buffer.setLength(0);
                } else
                    buffer.append((char) ch);
            }
        };
    }
}