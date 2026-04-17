package illa4257.i4Utils.nio.web.tasks;

import illa4257.i4Utils.lists.ArrStack;

import java.nio.ByteBuffer;

public class BuffLand {
    public static final int S0 = 13, S1 = 32 * 1024, S2 = 1024 * 1024, BIGGEST = S2;

    private final ArrStack<ByteBuffer> bl0 = new ArrStack<>(), bl1 = new ArrStack<>(), bl2 = new ArrStack<>();

    public ByteBuffer bl0() {
        final ByteBuffer b = bl0.pop();
        return b != null ? b : ByteBuffer.allocateDirect(S0);
    }

    public ByteBuffer bl1() {
        final ByteBuffer b = bl1.pop();
        return b != null ? b : ByteBuffer.allocateDirect(S1);
    }

    public ByteBuffer bl2() {
        final ByteBuffer b = bl2.pop();
        return b != null ? b : ByteBuffer.allocateDirect(S2);
    }

    public void bl0(final ByteBuffer buffer) {
        buffer.clear();
        bl0.push(buffer);
    }

    public void bl1(final ByteBuffer buffer) {
        buffer.clear();
        bl1.push(buffer);
    }

    public void bl2(final ByteBuffer buffer) {
        buffer.clear();
        bl2.push(buffer);
    }

    public void recycle(final ByteBuffer buffer) {
        buffer.clear();
        (buffer.capacity() < S1 ? bl0 : buffer.capacity() < S2 ? bl1 : bl2).push(buffer);
    }

    public ByteBuffer auto(final int length) {
        return length <= BuffLand.S0 ? bl0() : length <= BuffLand.S1 ? bl1() : bl2();
    }
}