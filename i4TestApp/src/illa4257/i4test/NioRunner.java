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
                    final Session s = (Session) key.attachment();
                    try {
                        if (s == null)
                            continue;
                        if (s.netOut != null) {
                            s.client.write(s.netOut);
                            if (s.netOut.hasRemaining())
                                return;
                            mgr.recycle(s.netOut);
                            s.netOut = null;
                            if (s.close) {
                                s.close();
                                continue;
                            }
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        s.process();
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.e(ex);
                        netOut.clear();
                        netIn.clear();
                        appOut.clear();
                        appIn.clear();
                        s.close();
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
        return channel.register(selector, SelectionKey.OP_READ);
    }

    public void close() {
        try {
            selector.close();
        } catch (IOException ex) {
            i4Logger.INSTANCE.e(ex);
        }
    }

    public abstract void accept(final SelectionKey key) throws IOException;

    public abstract static class Session {
        public SelectionKey key = null;
        public SSLEngine engine = null;
        public NioRunner runner = null;
        public BuffMgr mgr = null;
        public SocketChannel client = null;
        public boolean closed = false, close = false;
        public ByteBuffer netIn = null, appIn = null, netOut = null, appOut = null;

        public abstract void process() throws Exception;

        public ByteBuffer getAppIn() {
            return appIn != null ? appIn : runner.appIn;
        }

        public int read() throws IOException {
            if (engine == null) {
                ByteBuffer ai = getAppIn();
                if (ai.capacity() == 0) {
                    ai = mgr.get();
                    if (appIn == null)
                        runner.appIn = ai;
                    else
                        appIn = ai;
                }
                return client.read(ai);
            }
            if (closed)
                return -1;

            l:while (true) {
                switch (engine.getHandshakeStatus()) {
                    case NEED_UNWRAP:
                        while (true) {
                            final ByteBuffer ni = netIn != null ? netIn : runner.netIn;
                            final int l = client.read(ni);
                            if (l == -1)
                                throw new IOException("Closed");
                            if (l == 0 && ni.position() == ni.capacity()) {
                                final ByteBuffer nb = mgr.nextTier(ni, engine.getSession().getPacketBufferSize());
                                nb.put(ni);
                                if (ni == runner.netIn)
                                    runner.netIn = nb;
                                else
                                    netIn = nb;
                                mgr.recycle(ni);
                                continue;
                            }
                            if (l == 0 && ni.position() == 0) {
                                key.interestOps(SelectionKey.OP_READ);
                                return 0;
                            }
                            ni.flip();
                            final SSLEngineResult r = engine.unwrap(ni, EMPTY_BUFFER);
                            switch (r.getStatus()) {
                                case OK:
                                    ni.compact();
                                    if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)
                                        continue;
                                    if (ni.position() != 0 && ni == runner.netIn) {
                                        netIn = ni;
                                        runner.netIn = mgr.get(engine.getSession().getPacketBufferSize());
                                    }
                                    break;
                                case BUFFER_UNDERFLOW:
                                    if (ni.remaining() == ni.capacity()) {
                                        final ByteBuffer nb = mgr.nextTier(ni, engine.getSession().getPacketBufferSize());
                                        nb.put(ni);
                                        if (ni == runner.netIn)
                                            ni.clear();
                                        else
                                            mgr.recycle(ni);
                                        netIn = nb;
                                    } else {
                                        key.interestOps(SelectionKey.OP_READ);
                                        ni.compact();
                                        if (ni == runner.netIn)
                                            runner.netIn = mgr.get(engine.getSession().getPacketBufferSize());
                                        netIn = ni;
                                        return 0;
                                    }
                                    continue;
                                case CLOSED:
                                    if (netIn == null)
                                        runner.netIn.clear();
                                    close();
                                    return -1;
                                default:
                                    throw new RuntimeException("Unknown unwrap result: " + r.getStatus());
                            }
                            break;
                        }
                        break;
                    case NEED_WRAP:
                        while (true) {
                            final ByteBuffer no = netOut != null ? netOut : runner.netOut;
                            final SSLEngineResult r = engine.wrap(EMPTY_BUFFER, no);
                            switch (r.getStatus()) {
                                case OK:
                                    no.flip();
                                    client.write(no);
                                    if (no.hasRemaining()) {
                                        key.interestOps(SelectionKey.OP_WRITE);
                                        netOut = no;
                                        if (no == runner.netOut)
                                            runner.netOut = mgr.get(engine.getSession().getPacketBufferSize());
                                        return 0;
                                    }
                                    if (no == runner.netOut)
                                        no.clear();
                                    else {
                                        netOut = null;
                                        mgr.recycle(no);
                                    }
                                    if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                                        continue;
                                    break;
                                case BUFFER_OVERFLOW:
                                    no.flip();
                                    final ByteBuffer nb = mgr.nextTier(no, engine.getSession().getPacketBufferSize());
                                    nb.put(no);
                                    if (no == runner.netOut)
                                        no.clear();
                                    else
                                        mgr.recycle(no);
                                    netOut = nb;
                                    continue;
                                case CLOSED:
                                    if (netOut == null)
                                        runner.netOut.clear();
                                    close();
                                    return -1;
                                default:
                                    throw new IOException("Unknown wrap status: " + r.getStatus());
                            }
                            break;
                        }
                        break;
                    case NEED_TASK:
                        key.interestOps(0);
                        runner.sslPool.submit(() -> {
                            try {
                                Runnable t;
                                while ((t = engine.getDelegatedTask()) != null)
                                    t.run();
                                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                runner.selector.wakeup();
                            } catch (final Exception ex) {
                                i4Logger.INSTANCE.e(ex);
                                close();
                            }
                        });
                        return 0;
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        break l;
                    default:
                        throw new RuntimeException("Unknown handshake status: " + engine.getHandshakeStatus());
                }
            }

            key.interestOps(SelectionKey.OP_READ);

            ByteBuffer ni = netIn != null ? netIn : runner.netIn, ai = getAppIn();
            int l = client.read(ni);
            ni.flip();

            if (ni.hasRemaining()) {
                while (true) {
                    final SSLEngineResult r = engine.unwrap(ni, ai);
                    switch (r.getStatus()) {
                        case OK:
                            ni.compact();
                            return r.bytesProduced();
                        case BUFFER_UNDERFLOW:
                            if (ni.position() == ni.capacity()) {
                                final ByteBuffer nb = mgr.nextTier(ni, engine.getSession().getPacketBufferSize());
                                nb.put(ni);
                                if (netIn != null)
                                    netIn = nb;
                                else
                                    runner.netIn = nb;
                                mgr.recycle(ni);
                                ni = nb;
                            } else {
                                if (l == -1) {
                                    closed = true;
                                    ni.clear();
                                    if (netIn != null)
                                        mgr.recycle(ni);
                                    return -1;
                                }
                                ni.compact();
                                return 0;
                            }
                            continue;
                        case BUFFER_OVERFLOW:
                            final ByteBuffer nb = mgr.nextTier(ai, engine.getSession().getApplicationBufferSize());
                            ai.flip();
                            nb.put(ai);
                            if (ai == runner.appIn)
                                runner.appIn = nb;
                            else
                                appIn = nb;
                            mgr.recycle(ai);
                            ai = nb;
                            continue;
                        default:
                            throw new RuntimeException("Unknown unwrap status: " + r.getStatus());
                    }
                }
            }
            if (ni == runner.netIn)
                ni.clear();
            else {
                netIn = null;
                mgr.recycle(ni);
            }
            if (l == -1)
                closed = true;
            return l;
        }

        public boolean write(final ByteBuffer buffer) throws IOException {
            if (engine == null) {
                client.write(buffer);
                if (buffer.hasRemaining()) {
                    netOut = buffer;
                    key.interestOps(SelectionKey.OP_WRITE);
                }
                return buffer.hasRemaining();
            }
            final SSLEngineResult r = engine.wrap(buffer, netOut);
            switch (r.getStatus()) {
                default:
                    throw new RuntimeException("Unknown status: " + r.getStatus());
            }
        }

        public void compactAppIn() {
            if (appIn != null) {
                if (appIn.hasRemaining())
                    appIn.compact();
                else {
                    mgr.recycle(appIn);
                    appIn = null;
                }
                return;
            }
            if (runner.appIn.hasRemaining()) {
                appIn = runner.appIn;
                appIn.compact();
                runner.appIn = engine != null ? mgr.get(engine.getSession().getApplicationBufferSize()) : mgr.get();
            } else
                runner.appIn.clear();
        }

        public int old = -1;
        public void async() {
            old = key.interestOps();
            key.interestOps(0);
        }

        public void resume() {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            runner.selector.wakeup();
        }

        public void reset() {
            old = -1;
            close = closed = false;
        }

        public void close() {
            closed = true;
            if (key != null) {
                key.cancel();
                key = null;
            }
            if (client != null) {
                try {
                    client.close();
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.e(ex);
                }
                client = null;
            }
            engine = null;
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