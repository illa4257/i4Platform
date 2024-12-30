package illa4257.i4Utils.web;

import java.io.IOException;
import java.io.InputStream;

public abstract class WebInputStream extends InputStream {
    protected final InputStream inputStream;

    public WebInputStream(final InputStream inputStream) {
        if (inputStream == null)
            throw new IllegalArgumentException("InputStream cannot be null");
        this.inputStream = inputStream;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(final byte[] bytes, final int i, final int i1) throws IOException {
        return inputStream.read(bytes, i, i1);
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        super.close();
    }

    public static class Chunked extends WebInputStream {
        private boolean finished = false, first = false;
        private Character prev = null;
        private int chunkSize = 0;

        private static final char[] allowed = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f',
                'A', 'B', 'C', 'D', 'E', 'F'
        };

        public Chunked(final InputStream inputStream) { super(inputStream); }

        private char readChar() throws IOException {
            if (prev != null) {
                final char r = prev;
                prev = null;
                return r;
            }
            final int r = inputStream.read();
            if (r == -1)
                throw new IOException("End");
            return (char) r;
        }

        private void readChunkSize() throws IOException {
            if (first) {
                char n = readChar();
                if (n == '\r')
                    n = readChar();
                if (n != '\n')
                    prev = n;
            }
            final StringBuilder b = new StringBuilder();
            boolean r = false;
            m:
            for (int i = 0; i < 7; i++) {
                final char ch = readChar();
                if (ch == '\r') {
                    r = true;
                    continue;
                }
                if (r || ch == '\n') {
                    if (r && ch != '\n')
                        prev = ch;
                    first = true;
                    if (b.length() == 0)
                        finished = true;
                    else {
                        final int re = Integer.parseInt(b.toString(), 16);
                        if (re <= 0)
                            finished = true;
                        else
                            chunkSize = re;
                    }
                    return;
                }
                for (final char c : allowed)
                    if (c == ch) {
                        b.append(ch);
                        continue m;
                    }
                throw new IOException("Unknown character for chunk size: " + ch);
            }
        }

        @Override
        public int read() throws IOException {
            if (finished)
                return -1;
            while (chunkSize <= 0) {
                readChunkSize();
                if (finished)
                    return -1;
            }
            chunkSize--;
            return inputStream.read();
        }

        @Override
        public int read(final byte[] bytes, final int i, final int i1) throws IOException {
            if (finished)
                return -1;
            while (chunkSize <= 0) {
                readChunkSize();
                if (finished)
                    return -1;
            }
            final int r = inputStream.read(bytes, i, Math.min(i1, chunkSize));
            chunkSize -= r;
            return r;
        }
    }

    public static class LongPolling extends WebInputStream {
        private int remaining;

        public LongPolling(final InputStream inputStream, final int length) {
            super(inputStream);
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
        public int read(byte[] bytes, final int i, final int i1) throws IOException {
            if (remaining <= 0)
                return -1;
            final int r = inputStream.read(bytes, i, Math.min(remaining, i1));
            remaining -= r;
            return r;
        }
    }
}