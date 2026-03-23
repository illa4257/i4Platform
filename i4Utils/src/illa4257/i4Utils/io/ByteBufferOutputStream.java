package illa4257.i4Utils.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private final ByteBuffer buffer;

    public ByteBufferOutputStream(final ByteBuffer buffer) { this.buffer = buffer; }

    @Override
    public void write(final int b) throws IOException {
        buffer.put((byte) b);
    }
}
