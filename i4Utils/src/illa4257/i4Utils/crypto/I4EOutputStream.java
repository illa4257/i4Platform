package illa4257.i4Utils.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Random;

public class I4EOutputStream extends OutputStream {
    private final int dataMaxLength;
    private final Random randomizer;
    private final OutputStream stream;
    private final byte[] sign, randomData;
    private int signIndex, len = 0, status = 0;

    /**
     *
     * @param stream
     * @param randomizer Use {@link SecureRandom}
     * @param sign
     * @param signIndex
     * @param dataMaxLength
     */
    public I4EOutputStream(final OutputStream stream, final Random randomizer, final byte[] sign, final int signIndex, final int dataMaxLength) {
        this.dataMaxLength = dataMaxLength + 1;
        this.stream = stream;
        this.randomizer = randomizer;
        this.sign = sign;
        this.signIndex = signIndex;
        this.randomData = new byte[dataMaxLength];
    }

    public I4EOutputStream(final OutputStream stream, final byte[] sign) { this(stream, new SecureRandom(), sign, 0, 128); }

    private void internalWrite(int b) throws IOException {
        final byte by = (byte) b;
        if (signIndex >= sign.length)
            signIndex = 0;
        stream.write(by + sign[signIndex++]);
    }

    private void writeInt(final int n) throws IOException {
        internalWrite(n >> 24);
        internalWrite(n >> 16);
        internalWrite(n >> 8);
        internalWrite(n);
    }

    private void sendRandomPacket() throws IOException {
        randomizer.nextBytes(randomData);
        final int l = randomizer.nextInt(dataMaxLength);
        writeInt(l);
        stream.write(randomData, 0, l);
    }

    private void status() throws IOException {
        if (status == 1) {
            if (len > 0)
                return;
            sendRandomPacket();
        }
        sendRandomPacket();
        len = randomizer.nextInt(dataMaxLength);
        if (len == 0)
            len++;
        writeInt(len);
        status = 1;
    }

    @Override
    public void write(int b) throws IOException {
        status();
        internalWrite(b);
        len--;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        status();
        while (len > this.len) {
            for (int i2 = 0; i2 < this.len; off++, i2++) {
                if (signIndex >= sign.length)
                    signIndex = 0;
                randomData[i2] = (byte) (b[off] + sign[signIndex++]);
            }
            stream.write(randomData, 0, this.len);
            len -= this.len;
            this.len = 0;
            status();
        }
        final int l = off + len;
        for (int i = off; i < l; i++) {
            if (signIndex >= sign.length)
                signIndex = 0;
            randomData[i] = (byte) (b[i] + sign[signIndex++]);
        }
        stream.write(randomData, off, len);
        this.len -= len;
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}