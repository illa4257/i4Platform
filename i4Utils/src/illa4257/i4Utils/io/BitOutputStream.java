package illa4257.i4Utils.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream extends OutputStream implements Closeable {
    public final OutputStream outputStream;

    public byte b = 0, p = 0;

    public BitOutputStream(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void write(final boolean bit) throws IOException {
        b <<= 1;
        b |= bit ? 1 : (byte) 0;
        if (p == 7) {
            outputStream.write(b);
            b = 0;
            p = 0;
            return;
        }
        p++;
    }

    public void write(final byte byt) throws IOException {
        final int val = byt & 0xFF;
        if (p == 0) {
            outputStream.write(val);
            return;
        }
        final int off = 8 - p;
        outputStream.write((b << off) | (val >>> p));
        b = (byte) (val & ((1 << p) - 1));
    }

    @Override
    public void write(int i) throws IOException {
        write((byte) i);
    }

    public void write(final boolean[] arr) throws IOException {
        for (final boolean b : arr)
            write(b);
    }

    @Override
    public void close() throws IOException {
        if (p > 0) {
            outputStream.write(b << (8 - p));
            b = 0;
            p = 0;
        }
        outputStream.close();
    }
}