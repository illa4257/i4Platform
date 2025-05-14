package illa4257.i4Utils.web;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.SyncVar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/// @deprecated
public class WebStream implements AutoCloseable {
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public final Socket socket;
    public final OutputStream outputStream;

    protected final InputStream inputStream;

    public final SyncVar<Charset> charset = new SyncVar<>();

    public WebStream(final Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
    }

    public void write(final String string) throws IOException {
        outputStream.write(string.getBytes(charset.get(DEFAULT_CHARSET)));
    }

    public void write(final char[] chars) throws IOException {
        outputStream.write(IO.toBytes(charset.get(DEFAULT_CHARSET), chars));
    }

    public void write(final byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    public void flush() throws IOException { outputStream.flush(); }

    private Character prev = null;

    protected char readChar() throws IOException {
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

    protected String readStr(final char ch, int max) throws IOException {
        final StringBuilder b = new StringBuilder();
        for (; max > 0; max--) {
            final char n = readChar();
            if (n == ch)
                return b.toString();
            b.append(n);
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    protected String readStrLn(int max) throws IOException {
        boolean r = false;
        final StringBuilder b = new StringBuilder();
        for (; max > 0; max--) {
            final char n = readChar();
            if (n == '\r') {
                r = true;
                continue;
            }
            if (n == '\n')
                return b.toString();
            if (r) {
                prev = n;
                return b.toString();
            }
            b.append(n);
        }
        if (r)
            prev = '\r';
        throw new IOException("Reached the maximum number of characters.");
    }

    protected String readStrLn(final char ch, int max) throws IOException {
        boolean r = false;
        final StringBuilder b = new StringBuilder();
        for (; max > 0; max--) {
            final char n = readChar();
            if (n == '\r') {
                r = true;
                continue;
            }
            if (n == '\n')
                return b.toString();
            if (r) {
                prev = n;
                return b.toString();
            }
            if (n == ch)
                return b.toString();
            b.append(n);
        }
        if (r)
            prev = '\r';
        throw new IOException("Reached the maximum number of characters.");
    }

    protected void skipSpaces(int max) throws IOException {
        for (max++; max > 0; max--) {
            final char n = readChar();
            if (n != ' ') {
                prev = n;
                return;
            }
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    @Override public void close() throws IOException { socket.close(); }
}