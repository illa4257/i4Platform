package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

public class ByteBufferInputStream extends InputStream {
    private final ByteBuffer buffer;

    public ByteBufferInputStream(final ByteBuffer buffer) { this.buffer = buffer; }

    @Override public int available() { return buffer.remaining(); }
    @Override public int read() throws IOException { return buffer.hasRemaining() ? buffer.get() & 0xFF : -1; }

    @Override
    public int read(@SuppressWarnings("NullableProblems") final byte[] bytes, final int off, final int len) throws IOException {
        if (bytes == null)
            throw new NullPointerException();
        if (off < 0 || len < 0 || len > bytes.length - off)
            throw new IndexOutOfBoundsException();
        if (!buffer.hasRemaining())
            return -1;
        final int l = Math.min(len, buffer.remaining());
        if (l == 0)
            return l;
        buffer.get(bytes, off, l);
        return l;
    }

    @Override
    public int read(@SuppressWarnings("NullableProblems") final byte[] bytes) throws IOException {
        if (bytes == null)
            throw new NullPointerException();
        if (!buffer.hasRemaining())
            return -1;
        final int l = Math.min(bytes.length, buffer.remaining());
        if (l == 0)
            return l;
        buffer.get(bytes, 0, l);
        return l;
    }

    @Override public boolean markSupported() { return true; }
    @Override public synchronized void mark(int i) { buffer.mark(); }

    @Override
    public synchronized void reset() throws IOException {
        try {
            buffer.reset();
        } catch (final InvalidMarkException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }
}