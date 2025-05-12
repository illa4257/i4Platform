package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.InputStream;

public class I4EInputStream extends InputStream {
    private final InputStream stream;
    private final byte[] sign;
    private int signIndex;
    private int status = 0, len = 0;

    public I4EInputStream(final InputStream stream, final byte[] sign, final int signIndex) {
        this.stream = stream;
        this.sign = sign;
        this.signIndex = signIndex;
    }

    public I4EInputStream(final InputStream stream, final byte[] sign) { this(stream, sign, 0); }

    private int internalRead() throws IOException {
        if (signIndex >= sign.length)
            signIndex = 0;
        final int r = stream.read();
        if (r == -1)
            return -1;
        return ((byte) r - sign[signIndex++]) & 0xFF;
    }

    private int readInt() throws IOException {
        return (internalRead() << 24) + (internalRead() << 16) + (internalRead() << 8) + internalRead();
    }

    private void status() throws IOException {
        if (status == 1) {
            if (len > 0)
                return;
            stream.skip(readInt());
        }
        stream.skip(readInt());
        len = readInt();
        status = 1;
    }

    @Override
    public int read() throws IOException {
        status();
        final int r = internalRead();
        if (r > -1)
            len--;
        return r;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}