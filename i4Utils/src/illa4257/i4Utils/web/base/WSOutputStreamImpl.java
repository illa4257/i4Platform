package illa4257.i4Utils.web.base;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.web.WSOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class WSOutputStreamImpl extends WSOutputStream {
    public static final SecureRandom RND = new SecureRandom();

    public final OutputStream outputStream;
    public volatile boolean masking = true;
    private final byte[] mask = new byte[4];

    public WSOutputStreamImpl(final OutputStream outputStream) { this.outputStream = outputStream; }

    public void writeType(final boolean isFinal, final int b) throws IOException { outputStream.write(isFinal ? b | 0x80 : b); }

    public void writeLength(final boolean masking, final long len) throws IOException {
        if (len <= 125) {
            outputStream.write(masking ? (int) len | 0x80 : (int) len);
            return;
        }
        if (len < 65536) {
            outputStream.write(masking ? 0xFE : 0x7E);
            IO.writeBEShort(outputStream, (int) len);
            return;
        }
        outputStream.write(masking ? 0xFF : 0x7F);
        IO.writeBELong(outputStream, len);
    }

    public void mask(final byte[] arr, int from, int len, int i) {
        for (; i < len; i++, from++)
            arr[from] ^= mask[i % 4];
    }

    public void mask(final byte[] arr, int from, int len) {
        for (int i = 0; i < len; i++, from++)
            arr[from] ^= mask[i % 4];
    }

    public void mask(final byte[] arr) {
        for (int i = 0; i < arr.length; i++)
            arr[i] ^= mask[i % 4];
    }

    @Override
    public synchronized void write(final int b) throws IOException {
        writeType(true, 0x02);
        final boolean masking = this.masking;
        writeLength(masking, 1);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            outputStream.write(b ^ mask[0]);
        } else
            outputStream.write(b);
    }

    @Override
    public synchronized void write(@SuppressWarnings("NullableProblems") final byte[] b, final int off, final int len) throws IOException {
        writeType(true, 0x02);
        final boolean masking = this.masking;
        writeLength(masking, len);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(b, off, len);
        }
        outputStream.write(b, off, len);
    }

    @Override
    public synchronized void write(final byte[] b) throws IOException {
        writeType(true, 0x02);
        final boolean masking = this.masking;
        writeLength(masking, b.length);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(b);
        }
        outputStream.write(b);
    }

    @Override
    public synchronized void write(final String str) throws IOException {
        writeType(true, 0x01);
        final boolean masking = this.masking;
        final byte[] arr = str.getBytes(StandardCharsets.UTF_8);
        writeLength(masking, arr.length);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(arr);
        }
        outputStream.write(arr);
    }

    /// Server-Side
    public synchronized void ping(final byte[] payload) throws IOException {
        writeType(true, 0x09);
        final boolean masking = this.masking;
        writeLength(masking, payload.length);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(payload);
        }
        outputStream.write(payload);
    }

    /// Client-Side, payload should the same as the ping payload.
    public synchronized void pong(final byte[] payload) throws IOException {
        writeType(true, 0x0A);
        final boolean masking = this.masking;
        writeLength(masking, payload.length);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(payload);
        }
        outputStream.write(payload);
    }

    @Override public synchronized void flush() throws IOException { outputStream.flush(); }

    @Override
    public synchronized void close(final int code, final String reason) throws IOException {
        writeType(true, 0x08);
        final boolean masking = this.masking;
        final byte[] c = new byte[] { (byte) (code >> 8), (byte) code };
        final byte[] r = reason == null || reason.isEmpty() ? new byte[0] : reason.getBytes(StandardCharsets.UTF_8);
        writeLength(masking, r.length + 2);
        if (masking) {
            RND.nextBytes(mask);
            outputStream.write(mask);
            mask(c);
            mask(r, 0, r.length, c.length);
        }
        outputStream.write(c);
        outputStream.write(r);
        outputStream.flush();
    }
}
