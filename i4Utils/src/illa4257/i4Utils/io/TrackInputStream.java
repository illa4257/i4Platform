package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.InputStream;

public class TrackInputStream extends InputStream {
    public final InputStream inputStream;
    public long position, mark = -1;

    public TrackInputStream(final InputStream stream) {
        inputStream = stream;
        position = 0;
    }

    public TrackInputStream(final InputStream stream, final int position) {
        inputStream = stream;
        this.position = position;
    }

    public long getPosition() { return position; }

    @Override
    public int read() throws IOException {
        final int r = inputStream.read();
        if (r > -1) position++;
        return r;
    }

    @Override
    public int read(final byte[] bytes) throws IOException {
        final int r = inputStream.read(bytes);
        position += r;
        return r;
    }

    @Override
    public int read(final byte[] bytes, final int i, final int i1) throws IOException {
        final int r = inputStream.read(bytes, i, i1);
        position += r;
        return r;
    }

    @Override
    public long skip(final long l) throws IOException {
        final long r = inputStream.skip(l);
        position += r;
        return r;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public synchronized void mark(final int i) {
        inputStream.mark(i);
        mark = position;
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
        position = mark;
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }
}