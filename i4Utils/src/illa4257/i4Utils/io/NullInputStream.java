package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.InputStream;

public final class NullInputStream extends InputStream {
    @Override public int read() { return -1; }
    @Override public int read(byte[] b) throws IOException { return -1; }
    @Override public int read(byte[] b, int off, int len) throws IOException { return -1; }
    @Override public long skip(long n) { return 0; }
}