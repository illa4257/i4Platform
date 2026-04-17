package illa4257.i4Utils.nio.web.tasks;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class CopyReplyTask extends QTask {
    public final ByteBuffer buffer;
    public long amount;

    public CopyReplyTask(final ByteBuffer buffer, final long amount) {
        this.buffer = buffer;
        this.amount = amount;
    }

    @Override
    public void tick() throws Exception {
        int l = transport.read(buffer);
        buffer.flip();
        if (l == -1 && !buffer.hasRemaining())
            throw new RuntimeException("EOS");
        if (amount > buffer.remaining() || amount < 0) {
            final int op = buffer.position();
            transport.write(buffer);
            amount -= buffer.position() - op;
            if (buffer.hasRemaining()) {
                buffer.compact();
                transport.interestOps(SelectionKey.OP_WRITE);
            } else {
                buffer.clear();
                transport.interestOps(SelectionKey.OP_READ);
            }
            return;
        }
        final ByteBuffer s = buffer.slice();
        s.limit((int) amount);
        transport.write(buffer);
        buffer.position(buffer.position() + s.position());
        amount -= s.position();
        if (s.hasRemaining()) {
            buffer.compact();
            transport.interestOps(SelectionKey.OP_WRITE);
            return;
        }
        if (buffer.hasRemaining())
            buffer.compact();
        else
            buffer.clear();
        complete();
    }
}