package illa4257.i4Utils.integration;

import java.io.IOException;
import java.io.InputStream;

public class CheerpJBYOBReader extends InputStream {
    static { System.loadLibrary("i4Utils.cheerpj"); }

    public final Object reader;

    public CheerpJBYOBReader(final Object reader) { this.reader = reader; }

    private native byte[] read(final int len);

    @Override
    public int read() throws IOException {
        final byte[] r = read(1);
        if (r == null)
            return -1;
        return r[0] & 0xFF;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final byte[] r = read(len);
        if (r == null)
            return -1;
        System.arraycopy(r, 0, b, off, r.length);
        return r.length;
    }

    @Override public native void close() throws IOException;
}