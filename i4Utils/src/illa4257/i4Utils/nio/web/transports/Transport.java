package illa4257.i4Utils.nio.web.transports;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface Transport {
    SelectionKey getSelectionKey();

    int interestOps();
    void interestOps(final int interest);

    void attach(final Object obj);
    Object attachment();

    void write(final ByteBuffer buffer) throws IOException;
    int read(final ByteBuffer buffer) throws IOException;
    void close() throws IOException;
}