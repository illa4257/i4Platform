package illa4257.i4test;

import java.nio.ByteBuffer;

public abstract class BuffMgr {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    public abstract ByteBuffer get();
    public abstract ByteBuffer get(final int min);
    public abstract ByteBuffer nextTier(final ByteBuffer buffer);
    public abstract ByteBuffer nextTier(final ByteBuffer buffer, final int min);
    public abstract void recycle(final ByteBuffer buffer);
}