package illa4257.i4Utils.web.base;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.web.WSInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class WSInputStreamImpl extends WSInputStream {
    public final InputStream inputStream;
    private int frameType, maskIndex = -1;
    private long remaining = -1;
    private final byte[] mask = new byte[4];
    private final Consumer<byte[]> onPing;
    private boolean masking = false;

    public WSInputStreamImpl(final InputStream inputStream, final Consumer<byte[]> onPing) { this.inputStream = inputStream; this.onPing = onPing; }

    @Override
    public int nextPacket() throws IOException {
        int b = inputStream.read();
        if (b == -1)
            return -1;
        if ((b | 0x80) == b)
            b ^= 0x80;
        final int frameType = b;
        if (frameType != 0x00)
            this.frameType = frameType;
        b = IO.readByteI(inputStream);
        masking = (b | 0x80) == b;
        if (masking) {
            b ^= 0x80;
            IO.readByteArray(inputStream, mask);
            maskIndex = 0;
        }
        if (b < 0x7E)
            remaining = b;
        else if (b == 0x7E)
            remaining = IO.readBEShortI(inputStream);
        else
            remaining = IO.readBELong(inputStream);
        if (frameType == 0x08) {
            if (remaining >= 2) {
                closeCode = IO.readBEShort(inputStream);
                remaining -= 2;
            } else
                closeCode = 1005;
        }
        return frameType;
    }

    public void skipRestPacket() throws IOException {
        if (remaining <= 0)
            return;
        //noinspection ResultOfMethodCallIgnored
        inputStream.skip(remaining);
        remaining = 0;
    }

    @Override
    public int read() throws IOException {
        if (closeCode != -1)
            return -1;
        if (stream) {
            while (remaining <= 0) {
                final int t = nextPacket();
                if (frameType == 0x01 || frameType == 0x02)
                    break;
                if (t == 0x09) {
                    if (onPing != null) {
                        if (remaining > Integer.MAX_VALUE || remaining < 0)
                            throw new IOException("More than integer " + remaining); // TODO: implement streaming
                        else if (masking) {
                            final byte[] payload = IO.readByteArray(inputStream, (int) remaining);
                            for (; maskIndex < mask.length; maskIndex++)
                                payload[maskIndex] ^= mask[maskIndex % mask.length];
                            onPing.accept(payload);
                        } else
                            onPing.accept(IO.readByteArray(inputStream, (int) remaining));
                    }
                    continue;
                }
                if (closeCode != -1)
                    return -1;
            }
        }
        if (remaining <= 0)
            return -1;
        remaining--;
        if (masking) {
            final int b = inputStream.read();
            return b != -1 ? b ^ mask[(maskIndex++) % mask.length] : -1;
        }
        return inputStream.read();
    }
}
