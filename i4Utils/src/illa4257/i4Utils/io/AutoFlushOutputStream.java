package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.OutputStream;

public class AutoFlushOutputStream extends OutputStream {
    public final OutputStream out;

    private int trigger = 2048, counter = 0;

    public AutoFlushOutputStream(final OutputStream out) { this.out = out; }

    public int getTrigger() { return trigger; }

    /// Set 0 or less to turn off trigger. When counter reaches the trigger number, it calls flush.
    public void setTrigger(final int size) { this.trigger = size; }

    @Override
    public void write(final int b) throws IOException {
        counter++;
        out.write(b);
        if (counter >= trigger && trigger > 0)
            flush();
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        counter += len;
        out.write(b, off, len);
        if (counter >= trigger && trigger > 0)
            flush();
    }

    @Override
    public void flush() throws IOException {
        counter = 0;
        out.flush();
    }

    @Override public void close() throws IOException { out.close(); }
}