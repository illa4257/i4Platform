package illa4257.i4test;

import illa4257.i4Utils.logger.i4Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import static illa4257.i4test.BuffMgr.EMPTY_BUFFER;

public abstract class NioRunner {
    public final ExecutorService sslPool;
    public final BuffMgr mgr;
    public final Selector selector;
    public final ConcurrentLinkedQueue<SelectionKey> triggerKeys = new ConcurrentLinkedQueue<>();

    public ByteBuffer netIn = EMPTY_BUFFER, netOut = EMPTY_BUFFER, appIn = EMPTY_BUFFER, appOut = EMPTY_BUFFER;

    public NioRunner(final ExecutorService sslPool, final BuffMgr buffMgr) throws IOException {
        selector = Selector.open();
        this.sslPool = sslPool;
        this.mgr = buffMgr;
    }

    public void run() {
        try {
            SelectionKey k;
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
                        key.cancel();
                        key.channel().close();
                        close(key);
                    }
                }
                selector.selectedKeys().clear();
                while ((k = triggerKeys.poll()) != null)
                    try {
                        process(k);
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.e(ex);
                        k.cancel();
                        k.channel().close();
                        close(k);
                    }
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

    public void wakeup(final SelectionKey key) {
        triggerKeys.offer(key);
        selector.wakeup();
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
        @SuppressWarnings("resource") final SocketChannel client = (SocketChannel) key.channel();
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

        ByteBuffer no = netOut, ni, ai;
        no.clear();

        if (s.netIn != null)
            ni = s.netIn;
        else
            (ni = netIn).clear();
        if (s.appIn != null)
            ai = s.appIn;
        else
            (ai = appIn).clear();

        if (s.engine != null) {
            while (!s.handshakeComplete) {
                switch (s.engine.getHandshakeStatus()) {
                    case NEED_WRAP:
                        final SSLEngineResult wrapResult = s.engine.wrap(EMPTY_BUFFER, no);
                        switch (wrapResult.getStatus()) {
                            case OK:
                                no.flip();
                                client.write(no);
                                if (no.hasRemaining()) {
                                    key.interestOps((key.interestOps() & ~SelectionKey.OP_READ) | SelectionKey.OP_WRITE);
                                    if (no == netOut)
                                        netOut = mgr.get(s.engine.getSession().getPacketBufferSize());
                                    s.netOut = no;
                                    return;
                                } else
                                    no.clear();
                                continue;
                            case BUFFER_OVERFLOW:
                                final ByteBuffer nb = mgr.nextTier(no, s.engine.getSession().getPacketBufferSize());
                                no.flip();
                                nb.put(no);
                                if (no != netOut)
                                    mgr.recycle(no);
                                else
                                    no.clear();
                                no = nb;
                                continue;
                            default:
                                throw new RuntimeException(wrapResult.toString());
                        }
                    case NEED_UNWRAP:
                        if (key.isReadable()) {
                            if (client.read(ni) == -1)
                                throw new IOException("Closed");
                            ni.flip();
                            final SSLEngineResult unwrapResult = s.engine.unwrap(ni, ai);
                            ni.compact();
                            switch (unwrapResult.getStatus()) {
                                case OK:
                                    if (ni.position() != 0) {
                                        netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                                        s.netIn = ni;
                                    }
                                    if (ai.position() != 0) {
                                        appIn = mgr.get(s.engine.getSession().getApplicationBufferSize());
                                        s.appIn = ai;
                                    }
                                    continue;
                                case BUFFER_OVERFLOW: {
                                    final ByteBuffer nb = mgr.nextTier(ai, s.engine.getSession().getApplicationBufferSize());
                                    ai.flip();
                                    nb.put(ai);
                                    if (ai != appIn)
                                        mgr.recycle(ai);
                                    else
                                        ai.clear();
                                    ai = nb;
                                    continue;
                                }
                                case BUFFER_UNDERFLOW:
                                    if (ni.position() == ni.capacity()) {
                                        final ByteBuffer nb = mgr.nextTier(ni, s.engine.getSession().getPacketBufferSize());
                                        ni.flip();
                                        nb.put(ni);
                                        if (ni != netIn)
                                            mgr.recycle(ni);
                                        else
                                            ni.clear();
                                        ni = nb;
                                        continue;
                                    }
                                    s.netIn = ni;
                                    if (ni == netIn)
                                        netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                                    return;
                            }
                            throw new IOException(unwrapResult.toString());
                        } else if (ni.hasRemaining()) {
                            ni.compact();
                            s.netIn = ni;
                            if (ni == netIn)
                                netIn = mgr.get(s.engine.getSession().getPacketBufferSize());
                        } else
                            ni.clear();
                        return;
                    case NEED_TASK:
                        final Runnable task = s.engine.getDelegatedTask();
                        if (task != null)
                            sslPool.submit(() -> {
                                try {
                                    task.run();
                                } finally {
                                    wakeup(key);
                                }
                            });
                        return;
                    case FINISHED:
                    case NOT_HANDSHAKING:
                        s.handshakeComplete = true;
                        break;
                    default:
                        throw new IOException(s.engine.getHandshakeStatus().toString());
                }
            }
        }



        throw new RuntimeException("Ready to process!");
    }

    public void close(final SelectionKey key) {
        final Session s = (Session) key.attachment();
        s.reset(mgr);
    }

    public static class Session {
        public SSLEngine engine = null;
        public boolean handshakeComplete = false;
        public ByteBuffer netIn = null, appIn = null, netOut = null, appOut = null;

        public void reset(final BuffMgr mgr) {
            engine = null;
            handshakeComplete = false;
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