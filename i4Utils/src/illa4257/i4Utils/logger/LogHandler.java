package illa4257.i4Utils.logger;

import illa4257.i4Utils.str.Str;
import illa4257.i4Utils.lists.ArrIterable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletionException;

public abstract class LogHandler implements ILogHandler {
    public final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal.withInitial(StringBuilder::new);

    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        final StringBuilder b = stringBuilder.get();
        b.setLength(0);
        b.append(message);
        for (final StackTraceElement element : stackTraceElements)
            b.append(System.lineSeparator()).append("\tat ").append(element);
        log(level, prefix, b.toString());
    }

    @Override
    public void log(final Level level, final String prefix, final Throwable throwable) {
        log(level, prefix, throwable.toString(), throwable.getStackTrace());
        if (throwable instanceof InvocationTargetException)
            log(level, prefix, ((InvocationTargetException) throwable).getTargetException());
        if (throwable instanceof CompletionException) {
            final Throwable cause = throwable.getCause();
            if (cause != null)
                log(level, prefix, cause);
        }
    }

    @Override
    public void log(final Level level, final String prefix, final Object... objects) {
        log(level, prefix, objects == null ? "null" : Str.join(" ", new ArrIterable<>(objects), Objects::toString));
    }

    @Override
    public void log(final Level level, final String prefix, final Object object) {
        log(level, prefix, object instanceof Object[] ?
                Arrays.hashCode((Object[]) object) + Arrays.toString((Object[]) object) : Objects.toString(object));
    }
}