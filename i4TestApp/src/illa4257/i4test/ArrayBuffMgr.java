package illa4257.i4test;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ArrayBuffMgr extends BuffMgr {
    public final int defaultBuffer;
    public final int[] sizes;
    public final ConcurrentLinkedDeque<ByteBuffer>[] buffers;

    /**
     * Fixed sizes of dequeues of byte buffers.
     * @param sizes Should be sorted.
     * @param defaultBuffer Default buffer tier when calling {@link ArrayBuffMgr#get()}.
     */
    public ArrayBuffMgr(final int[] sizes, final int defaultBuffer) {
        this.defaultBuffer = defaultBuffer;
        this.sizes = sizes;
        //noinspection unchecked
        buffers = (ConcurrentLinkedDeque<ByteBuffer>[]) new ConcurrentLinkedDeque[sizes.length];
        for (int i = 0; i < buffers.length; i++)
            buffers[i] = new ConcurrentLinkedDeque<>();
    }

    /// Override if it's possible mathematically calculate the tier.
    public int tier(final int size) {
        for (int i = 0; i < sizes.length; i++)
            if (sizes[i] >= size)
                return i;
        throw new RuntimeException("Higher than the maximum size");
    }

    @Override
    public ByteBuffer get() {
        final ByteBuffer b = buffers[defaultBuffer].poll();
        return b != null ? b : ByteBuffer.allocateDirect(sizes[defaultBuffer]);
    }

    @Override
    public ByteBuffer get(final int min) {
        final int tier = tier(min);
        final ByteBuffer b = buffers[tier].poll();
        return b != null ? b : ByteBuffer.allocateDirect(sizes[tier]);
    }

    @Override
    public ByteBuffer nextTier(final ByteBuffer buffer) {
        final int tier = tier(buffer.capacity()) + 1;
        if (tier >= buffers.length)
            throw new RuntimeException("Higher than the maximum size: " + buffer.capacity());
        final ByteBuffer b = buffers[tier].poll();
        return b != null ? b : ByteBuffer.allocateDirect(sizes[tier]);
    }

    @Override
    public ByteBuffer nextTier(final ByteBuffer buffer, final int min) {
        if (min > buffer.capacity())
            return get(min);
        return nextTier(buffer);
    }

    @Override
    public void recycle(final ByteBuffer buffer) {
        if (buffer.capacity() == 0)
            return;
        buffer.clear();
        buffers[tier(buffer.capacity())].push(buffer);
    }
}