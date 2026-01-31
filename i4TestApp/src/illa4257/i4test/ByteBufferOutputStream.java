package illa4257.i4test;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferOutputStream extends OutputStream {
    private final ByteBuffer buffer;

    public ByteBufferOutputStream(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int i) {
        buffer.put((byte) i);
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") byte[] bytes, int i, int i1) {
        buffer.put(bytes, i, i1);
    }

    @Override
    public void write(@SuppressWarnings("NullableProblems") byte[] bytes) {
        buffer.put(bytes);
    }

    public ByteBuffer getBuffer() { return buffer; }
}