package illa4257.i4Utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CloseableSyncVar<T> extends SyncVar<T> implements Closeable {
    public final AtomicBoolean preventClosing = new AtomicBoolean(false);

    public CloseableSyncVar() {}
    public CloseableSyncVar(final T value) { super(value); }

    @Override
    public void close() throws IOException {
        if (preventClosing.get())
            return;
        final T v = get();
        if (v == null)
            return;
        if (v instanceof Closeable)
            ((Closeable) v).close();
        else if (v instanceof AutoCloseable)
            try {
                ((AutoCloseable) v).close();
            } catch (final Exception ex) {
                if (ex instanceof IOException)
                    throw (IOException) ex;
                throw new IOException(ex);
            }
    }
}