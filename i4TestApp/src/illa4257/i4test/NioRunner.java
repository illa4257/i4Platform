package illa4257.i4test;

import illa4257.i4Utils.logger.i4Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;

import static illa4257.i4test.BuffMgr.EMPTY_BUFFER;

public abstract class NioRunner {
    public final ExecutorService sslPool;
    public final BuffMgr mgr;
    public final Selector selector;

    public ByteBuffer netIn = EMPTY_BUFFER, netOut = EMPTY_BUFFER, appIn = EMPTY_BUFFER, appOut = EMPTY_BUFFER;

    public NioRunner(final ExecutorService sslPool, final BuffMgr buffMgr) throws IOException {
        selector = Selector.open();
        this.sslPool = sslPool;
        this.mgr = buffMgr;
    }

    public void run() {
        try {
            while (selector.isOpen()) {
                selector.select();
                for (final SelectionKey key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        accept(key);
                        continue;
                    }
                    try {
                        process(key);
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.e(ex);
                        close(key);
                        netOut.clear();
                        netIn.clear();
                        appOut.clear();
                        appIn.clear();
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (final ClosedSelectorException | IOException ex) {
            i4Logger.INSTANCE.e(ex);
        }
    }

    public void register(final ServerSocketChannel channel) throws ClosedChannelException {
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public SelectionKey register(SocketChannel channel) throws ClosedChannelException {
        return channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void close() {
        try {
            selector.close();
        } catch (IOException ex) {
            i4Logger.INSTANCE.e(ex);
        }
    }

    public abstract void accept(final SelectionKey key) throws IOException;

    public void process(SelectionKey key) throws IOException {
        final SocketChannel client = (SocketChannel) key.channel();
        final Session s = (Session) key.attachment();

        if (s.netOut != null) {
            client.write(s.netOut);
            if (s.netOut.hasRemaining())
                return;
            mgr.recycle(s.netOut);
            s.netOut = null;
            if ((key.interestOps() & SelectionKey.OP_READ) == 0)
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        }

        if (s.engine != null)
            while (s.handshaking) {
                switch (s.engine.getHandshakeStatus()) {
                    case NEED_UNWRAP:
                        while (true) {
                            final ByteBuffer ni = s.netIn != null ? s.netIn : netIn;
                            final int l = client.read(ni);
                            if (l == -1)
                                throw new IOException("Closed");
                            if (l == 0 && ni.position() == ni.capacity()) {
                                final ByteBuffer nb = mgr.nextTier(ni, s.engine.getSession().getPacketBufferSize());
                                nb.put(ni);
                                if (ni == netIn)
                                    netIn = nb;
                                else
                                    s.netIn = nb;
                                mgr.recycle(ni);
                                continue;
                            }
                            if (l == 0 && ni.position() == 0) {
                                key.interestOps(SelectionKey.OP_READ);
                                return;
                            }
                            ni.flip();
                            final SSLEngineResult r = s.engine.unwrap(ni, EMPTY_BUFFER);
                            switch (r.getStatus()) {
                                case OK:
                                    ni.compact();
                                    if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)
                                        continue;
                                    if (ni.position() != 0 && ni == netIn) {
                                        s.netIn = ni;
                                        netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                                    }
                                    break;
                                case BUFFER_UNDERFLOW:
                                    if (ni.remaining() == ni.capacity()) {
                                        final ByteBuffer nb = mgr.nextTier(ni, s.engine.getSession().getPacketBufferSize());
                                        nb.put(ni);
                                        if (ni == netIn)
                                            ni.clear();
                                        else
                                            mgr.recycle(ni);
                                        s.netIn = nb;
                                    } else {
                                        key.interestOps(SelectionKey.OP_READ);
                                        ni.compact();
                                        if (ni == netIn)
                                            netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                                        s.netIn = ni;
                                        return;
                                    }
                                    continue;
                                case CLOSED:
                                    if (s.netIn == null)
                                        netIn.clear();
                                    close(key);
                                    return;
                                default:
                                    throw new RuntimeException("Unknown unwrap result: " + r.getStatus());
                            }
                            break;
                        }
                        break;
                    case NEED_WRAP:
                        while (true) {
                            final ByteBuffer no = s.netOut != null ? s.netOut : netOut;
                            final SSLEngineResult r = s.engine.wrap(EMPTY_BUFFER, no);
                            switch (r.getStatus()) {
                                case OK:
                                    no.flip();
                                    client.write(no);
                                    if (no.hasRemaining()) {
                                        key.interestOps(SelectionKey.OP_WRITE);
                                        s.netOut = no;
                                        if (no == netOut)
                                            netOut = mgr.get(s.engine.getSession().getPacketBufferSize());
                                        return;
                                    }
                                    if (no == netOut)
                                        no.clear();
                                    else {
                                        s.netOut = null;
                                        mgr.recycle(no);
                                    }
                                    if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                                        continue;
                                    break;
                                case BUFFER_OVERFLOW:
                                    no.flip();
                                    final ByteBuffer nb = mgr.nextTier(no, s.engine.getSession().getPacketBufferSize());
                                    nb.put(no);
                                    if (no == netOut)
                                        no.clear();
                                    else
                                        mgr.recycle(no);
                                    s.netOut = nb;
                                    continue;
                                case CLOSED:
                                    if (s.netOut == null)
                                        netOut.clear();
                                    close(key);
                                    return;
                                default:
                                    throw new IOException("Unknown wrap status: " + r.getStatus());
                            }
                            break;
                        }
                        break;
                    case NEED_TASK:
                        key.interestOps(0);
                        sslPool.submit(() -> {
                            try {
                                Runnable t;
                                while ((t = s.engine.getDelegatedTask()) != null)
                                    t.run();
                                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                selector.wakeup();
                            } catch (final Exception ex) {
                                i4Logger.INSTANCE.e(ex);
                                close(key);
                            }
                        });
                        return;
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        s.handshaking = false;
                        break;
                    default:
                        throw new RuntimeException("Unknown handshake status: " + s.engine.getHandshakeStatus());
                }
            }

        processData(key);
    }

    public abstract void processData(final SelectionKey key) throws IOException;

    public void close(final SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (final Exception ex) {
            i4Logger.INSTANCE.e(ex);
        }
        final Session s = (Session) key.attachment();
        s.reset(mgr);
    }

    public static class Session {
        public SSLEngine engine = null;
        public boolean handshaking = true;
        public ByteBuffer netIn = null, appIn = null, netOut = null, appOut = null;

        public void reset(final BuffMgr mgr) {
            engine = null;
            handshaking = true;
            if (netIn != null) {
                mgr.recycle(netIn);
                netIn = null;
            }
            if (netOut != null) {
                mgr.recycle(netOut);
                netOut = null;
            }
            if (appIn != null) {
                mgr.recycle(appIn);
                appIn = null;
            }
            if (appOut != null) {
                mgr.recycle(appOut);
                appOut = null;
            }
        }
    }
}