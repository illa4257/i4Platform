package illa4257.i4Utils.web.base;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class WebInputStream extends InputStream {
    public final InputStream inputStream;
    private final AtomicBoolean end = new AtomicBoolean(true);
    private final Runnable onEnd;

    public WebInputStream(final InputStream is, final Runnable onEnd) {
        inputStream = is;
        this.onEnd = onEnd;
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    protected void end() {
        if (end.getAndSet(false) && onEnd != null)
            onEnd.run();
    }

    public static class Chunked extends WebInputStream {
        private boolean isFinished = false, isClosed = false;
        private long remaining = 0;
        private int oldByte = '\n';
        private final StringBuilder b = new StringBuilder();

        public Chunked(final InputStream is, final Runnable onEnd) { super(is, onEnd); }

        private int readCh() throws IOException {
            if (oldByte != -1) {
                final int r = oldByte;
                oldByte = -1;
                return r;
            }
            return IO.readByteI(inputStream);
        }

        private void nextChunk() throws IOException {
            b.setLength(0);

            int ch = readCh();
            if (ch == '\r')
                ch = readCh();
            if (ch != '\n')
                oldByte = ch;

            boolean r = false;
            while (true) {
                ch = readCh();
                if (ch == '\n')
                    break;
                if (ch == '\r') {
                    r = true;
                    continue;
                }
                if (r) {
                    oldByte = ch;
                    break;
                }
                b.append((char) ch);
            }
            if (b.length() == 0) {
                isFinished = true;
                return;
            }
            remaining = Long.parseLong(b.toString(), 16);
            if (remaining == 0) {
                isFinished = true;
                ch = inputStream.read();
                if (ch == '\n')
                    return;
                if (ch == '\r' && inputStream.read() != '\n')
                    i4Logger.INSTANCE.log(Level.WARN, "Weird server.");
            }
        }

        @Override
        public int read() throws IOException {
            if (isFinished || isClosed)
                return -1;
            if (remaining == 0) {
                nextChunk();
                if (isFinished)
                    return -1;
            }
            if (oldByte != -1) {
                final int r = oldByte;
                oldByte = -1;
                remaining--;
                return r;
            }
            remaining--;
            final int r = inputStream.read();
            if (r == -1)
                isClosed = true;
            return r;
        }

        @Override
        public int read(@SuppressWarnings("NullableProblems") final byte[] b, int off, int len) throws IOException {
            if (isFinished || isClosed)
                return -1;
            if (remaining == 0) {
                nextChunk();
                if (isFinished)
                    return -1;
            }
            if (oldByte != -1) {
                b[off] = (byte) oldByte;
                oldByte = -1;
                if (len == 1) {
                    remaining--;
                    return 1;
                }
                off++;
                len--;
            }
            final int l = inputStream.read(b, off, (int) Math.min(len, remaining));
            if (l == -1) {
                isClosed = true;
                return -1;
            }
            remaining -= l;
            return l;
        }

        @Override
        public void close() throws IOException {
            while (!isFinished) {
                if (remaining == 0)
                    nextChunk();
                final long s = inputStream.skip(remaining);
                if (s == 0) {
                    isFinished = true;
                    break;
                }
                remaining -= s;
            }
            super.close();
            if (isClosed)
                inputStream.close();
            else
                end();
        }
    }

    public static class LongPolling extends WebInputStream {
        private long remaining;

        public LongPolling(final InputStream inputStream, final Runnable onEnd, final long length) {
            super(inputStream, onEnd);
            remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0)
                return -1;
            remaining--;
            return inputStream.read();
        }

        @Override
        public int read(@SuppressWarnings("NullableProblems") final byte[] bytes, final int i, final int i1) throws IOException {
            if (remaining <= 0)
                return -1;
            final int r = inputStream.read(bytes, i, (int) Math.min(remaining, i1));
            remaining -= r;
            return r;
        }

        @Override
        public void close() throws IOException {
            remaining -= inputStream.skip(remaining);
            super.close();
            if (remaining == 0)
                end();
            else
                inputStream.close();
        }
    }
}