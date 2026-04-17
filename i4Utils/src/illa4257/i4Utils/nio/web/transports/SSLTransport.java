package illa4257.i4Utils.nio.web.transports;

import illa4257.i4Utils.nio.web.WebServer;
import illa4257.i4Utils.nio.web.tasks.BuffLand;
import illa4257.i4Utils.nio.web.tasks.Task;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class SSLTransport extends RawTransport {
    private static final ByteBuffer ZERO = ByteBuffer.allocateDirect(0);

    public final SSLEngine engine;
    private int interestOps = 0;
    private final ByteBuffer netIn, netOut;
    private byte status = 0;
    private boolean allow = false;

    public SSLTransport(final SelectionKey key, final SSLEngine engine, final ByteBuffer feeder) {
        super(key);
        this.engine = engine;
        this.netIn = feeder;
        netOut = getWorker().land.bl1();
    }

    public SSLTransport(final Transport transport, final SSLEngine engine, final ByteBuffer feeder) {
        super(transport.getSelectionKey());
        this.engine = engine;
        this.netIn = feeder;
        interestOps = transport.interestOps();
        netOut = getWorker().land.bl1();
    }

    public WebServer.WebServerWorker getWorker() {
        return ((Task) key.attachment()).worker;
    }

    @Override public int interestOps() { return interestOps; }
    @Override public void interestOps(final int interest) {
        interestOps = interest;
        if (allow)
            //noinspection MagicConstant
            key.interestOps(interest);
    }

    public boolean check() throws IOException {
        SSLEngineResult.HandshakeStatus s = engine.getHandshakeStatus();
        SSLEngineResult r;
        while (true) {
            if (status == 1) {
                channel.write(netOut);
                if (netOut.hasRemaining())
                    return true;
                status = 0;
                netOut.clear();
            }
            if (status == 2) {
                final int l = channel.read(netIn);
                if (l == -1)
                    throw new IOException("End of stream");
                if (l == 0)
                    return true;
                status = 0;
                netIn.flip();
            }
            if (s == SSLEngineResult.HandshakeStatus.FINISHED || s == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
                break;
            if (allow)
                allow = false;
            switch (s) {
                case NEED_UNWRAP:
                    r = engine.unwrap(netIn, ZERO);
                    switch (r.getStatus()) {
                        case OK:
                            break;
                        case BUFFER_UNDERFLOW:
                            netIn.compact();
                            status = 2;
                            key.interestOps(SelectionKey.OP_READ);
                            return true;
                        default:
                            throw new RuntimeException("Unknown status " + r.getStatus());
                    }
                    s = r.getHandshakeStatus();
                    break;
                case NEED_TASK:
                    final WebServer.WebServerWorker worker = ((Task) key.attachment()).worker;
                    final WebServer server = worker.getServer();
                    key.interestOps(0);
                    server.runTask(() -> {
                        engine.getDelegatedTask().run();
                        key.interestOps(SelectionKey.OP_WRITE);
                        worker.selector.wakeup();
                    });
                    return true;
                case NEED_WRAP:
                    r = engine.wrap(ZERO, netOut);
                    switch (r.getStatus()) {
                        case OK:
                            break;
                        default:
                            throw new RuntimeException("Unknown status " + r.getStatus());
                    }
                    netOut.flip();
                    channel.write(netOut);
                    if (netOut.hasRemaining()) {
                        status = 1;
                        return true;
                    } else
                        netOut.clear();
                    s = r.getHandshakeStatus();
                    break;
                default:
                    throw new RuntimeException("Unknown handshake status " + s);
            }
        }
        //noinspection MagicConstant
        key.interestOps(interestOps);
        allow = true;
        return false;
    }

    @Override
    public void write(final ByteBuffer buffer) throws IOException {
        if (check())
            return;
        engine.wrap(buffer, netOut);
        netOut.flip();
        channel.write(netOut);
        if (netOut.hasRemaining())
            status = 1;
        else
            netOut.clear();
    }

    @Override
    public int read(final ByteBuffer buffer) throws IOException {
        if (check())
            return 0;
        netIn.compact();
        int l = channel.read(netIn);
        if (l == -1)
            return -1;
        netIn.flip();
        if (l == 0)
            return 0;
        return engine.unwrap(netIn, buffer).bytesProduced();
    }

    @Override
    public void close() throws IOException {
        engine.closeOutbound();
        engine.wrap(ZERO, netOut);
        netOut.flip();
        channel.write(netOut);
        final BuffLand l = getWorker().land;
        l.bl1(netIn);
        if (netOut.hasRemaining()) {
            key.attach(new Task() {
                @Override
                public void tick() throws Exception {
                    channel.write(netOut);
                    if (netOut.hasRemaining())
                        return;
                    l.bl1(netOut);
                    SSLTransport.super.close();
                }
            });
            return;
        }
        l.bl1(netOut);
        super.close();
    }
}