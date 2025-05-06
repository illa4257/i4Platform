package illa4257.i4Utils.logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FilterHandler extends LogHandler {
    public final ILogHandler logHandler;
    public volatile boolean isBlockList;
    public final ConcurrentLinkedQueue<Level> list;

    public FilterHandler(final ILogHandler logHandler, final boolean isBlockList, final Level... levels) {
        this.logHandler = logHandler;
        this.isBlockList = isBlockList;
        list = new ConcurrentLinkedQueue<>(Arrays.asList(levels));
    }

    public FilterHandler(final ILogHandler logHandler, final boolean isBlockList, final Collection<Level> levels) {
        this.logHandler = logHandler;
        this.isBlockList = isBlockList;
        list = new ConcurrentLinkedQueue<>(levels);
    }

    @Override
    public void log(final Level level, final String prefix, final String message) {
        if (list.contains(level) == isBlockList)
            return;
        logHandler.log(level, prefix, message);
    }

    @Override
    public void log(final Level level, final String prefix, final String message, final StackTraceElement[] stackTraceElements) {
        if (list.contains(level) == isBlockList)
            return;
        logHandler.log(level, prefix, message, stackTraceElements);
    }
}
