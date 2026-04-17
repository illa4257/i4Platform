package illa4257.i4Utils.nio.web.tasks;

import java.nio.ByteBuffer;

public class WriteTask extends QTask {
    public ByteBuffer buffer = null;
    public boolean recycle = false;

    public WriteTask set(final ByteBuffer buffer, final boolean recycle) {
        this.buffer = buffer;
        this.recycle = recycle;
        return this;
    }

    @Override
    public void tick() throws Exception {
        transport.write(buffer);
        if (buffer.hasRemaining())
            return;
        if (recycle)
            worker.land.recycle(buffer);
        complete();
    }

    @Override
    public void recycle() {
        if (recycle)
            worker.land.recycle(buffer);
        super.recycle();
    }
}