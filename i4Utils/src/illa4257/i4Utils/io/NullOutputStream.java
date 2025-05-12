package illa4257.i4Utils.io;

import java.io.OutputStream;

public final class NullOutputStream extends OutputStream {
    @Override public void write(final int b) {}
    @Override public void write(final byte[] b) {}
    @Override public void write(final byte[] b, final int off, final int len) {}
}