package i4Utils.logger;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class i4Logger implements LogHandler {
    private static final Object locker = new Object();
    private static i4Logger parent = null;

    public static boolean setParent(final i4Logger newParent) {
        synchronized (locker) {
            parent = newParent;
        }
        return newParent != null;
    }

    public static i4Logger getParent() {
        synchronized (locker) {
            return parent;
        }
    }

    public static final i4Logger INSTANCE = new i4Logger() {
        @Override
        public boolean registerHandler(final LogHandler handler) {
            return super.registerHandler(handler);
        }

        @Override
        public boolean unregisterHandler(final LogHandler handler) {
            return super.unregisterHandler(handler);
        }

        @Override
        public i4Logger sub(final String name) {
            return super.sub(name);
        }

        @Override
        public void log(final Level level, final String prefix, final String message) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, prefix, message);
        }

        @Override
        public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, prefix, message, stackTraceElements);
        }

        @Override
        public void log(final Level level, final String message) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, message);
        }

        @Override
        public void log(final Level level, final String message, final StackTraceElement[] stackTraceElements) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, message, stackTraceElements);
        }

        @Override
        public void log(final Level level, final Throwable throwable) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, throwable);
        }

        @Override
        public void log(final Throwable throwable) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(throwable);
        }

        @Override
        public void log(final Level level, final Object... objects) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, objects);
        }

        @Override
        public void log(final Level level, final Object object) {
            final i4Logger l = getParent();
            if (l == null)
                return;
            l.log(level, object);
        }

        @Override
        public OutputStream newOutputStream(final Level level) {
            return super.newOutputStream(level);
        }
    };

    public final String name;
    private final ConcurrentLinkedQueue<LogHandler> handlers = new ConcurrentLinkedQueue<>();

    public i4Logger() {
        this.name = null;
    }

    public i4Logger(final String name) {
        this.name = name;
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
}