package illa4257.i4Utils.logger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static illa4257.i4Utils.logger.Level.ERROR;
import static illa4257.i4Utils.logger.Level.INFO;

public class i4Logger extends LogHandler {
    public static final i4Logger INSTANCE;

    static {
        final String vendor = System.getProperty("java.vendor");
        INSTANCE = new i4Logger("i4Utils")
                .registerHandler(vendor != null && vendor.toLowerCase().contains("android") ?
                        new AndroidLogger() : new PrintStreamLogHandler(System.out));
    }

    public final String name;
    private final ConcurrentLinkedQueue<ILogHandler> handlers = new ConcurrentLinkedQueue<>();

    public i4Logger() { this.name = null; }
    public i4Logger(final String name) { this.name = name; }

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

    public PrintStream newPrintStream(final Level level) {
        return new PrintStream(newOutputStream(level)) {
            @Override public void println(final String x) { log(level, x); }
            @Override public void println(final Object obj) { log(level, obj); }
        };
    }

    public i4Logger registerHandler(final ILogHandler handler) {
        if (handler == null)
            return this;
        handlers.add(handler);
        return this;
    }

    public i4Logger unregisterHandler(final ILogHandler handler) {
        if (handler == null)
            return this;
        handlers.remove(handler);
        return this;
    }

    public i4Logger unregisterAllHandlers() { handlers.clear(); return this; }

    public i4Logger sub(final String name) {
        if (name == null)
            throw new NullPointerException();
        final i4Logger r = new i4Logger(this.name == null ? name : this.name + '/' + name);
        r.registerHandler(this);
        return r;
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        for (final ILogHandler handler : handlers)
            handler.log(level, prefix, message);
    }

    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        for (final ILogHandler handler : handlers)
            handler.log(level, prefix, message, stackTraceElements);
    }

    public void log(final Level level, final String message) { log(level, name, message); }
    public void log(final Level level, final String message, final StackTraceElement[] stackTraceElements) { log(level, name, message, stackTraceElements); }
    public void log(final Level level, final Throwable throwable) { log(level, name, throwable); }
    public void log(final Throwable throwable) { log(ERROR, name, throwable); }
    public void log(final Level level, final Object... objects) { log(level, name, objects); }
    public void log(final Level level, final Object object) { log(level, name, object); }

    public i4Logger inheritIO() {
        System.setOut(newPrintStream(INFO));
        System.setErr(newPrintStream(ERROR));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> log(e));

        LogManager.getLogManager().reset();
        Logger.getGlobal().setLevel(java.util.logging.Level.ALL);
        Logger.getGlobal().addHandler(new JavaLogCatcher(this));
        return this;
    }

    public i4Logger inheritGlobalIO() {
        i4Logger.INSTANCE.unregisterAllHandlers().registerHandler(this);
        return inheritIO();
    }
}