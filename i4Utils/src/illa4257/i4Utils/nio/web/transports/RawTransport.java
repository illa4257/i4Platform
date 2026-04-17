package illa4257.i4Utils.nio.web.transports;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RawTransport implements Transport {
    public final SelectionKey key;
    public final SocketChannel channel;

    public RawTransport(final SelectionKey key) {
        this.key = key;
        this.channel = (SocketChannel) key.channel();
    }

    @Override
    public SelectionKey getSelectionKey() {
        return key;
    }

    @Override
    public int interestOps() {
        return key.interestOps();
    }

    @Override
    public void interestOps(final int interest) {
        //noinspection MagicConstant
        key.interestOps(interest);
    }

    @Override
    public void attach(final Object obj) {
        key.attach(obj);
    }

    @Override
    public Object attachment() {
        return key.attachment();
    }

    @Override
    public void write(final ByteBuffer buffer) throws IOException {
        channel.write(buffer);
    }

    @Override
    public int read(final ByteBuffer buffer) throws IOException {
        return channel.read(buffer);
    }

    @Override
    public void close() throws IOException {
        channel.close();
        key.cancel();
    }
}
