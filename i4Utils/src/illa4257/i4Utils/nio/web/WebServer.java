package illa4257.i4Utils.nio.web;

import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.nio.web.tasks.AcceptTask;
import illa4257.i4Utils.nio.web.tasks.BuffLand;
import illa4257.i4Utils.nio.web.tasks.Protocol;
import illa4257.i4Utils.nio.web.tasks.Task;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class WebServer {
    public static final i4Logger L = new i4Logger("WebServer").registerHandler(i4Logger.INSTANCE);

    private volatile WebServerWorker[] threads = null;


    public abstract void runTask(final Runnable runnable);

    private static final AtomicInteger roundRobin = new AtomicInteger();

    public abstract SSLEngine createEngine();
    public abstract WebHandler getHandler(final String method, final String path, final String protocol, final Protocol p);

    public void accept(final SelectableChannel channel) {
        while (true) {
            final WebServerWorker[] l = threads;
            final WebServerWorker w = l[roundRobin.getAndIncrement() % l.length];
            if (!w.isAlive)
                continue;
            w.channels.offer(channel);
            w.selector.wakeup();
            return;
        }
    }

    public synchronized void set(final int n) throws IOException {
        if (n < 0)
            throw new IllegalArgumentException();
        if (threads == null) {
            final WebServerWorker[] nt = new WebServerWorker[n];
            for (int i = 0; i < n; i++)
                nt[i] = new WebServerWorker();
            threads = nt;
            return;
        }
        if (n == threads.length)
            return;
        final WebServerWorker[] nt = new WebServerWorker[n], ot = threads;
        if (n > ot.length) {
            System.arraycopy(ot, 0, nt, 0, ot.length);
            for (int i = 0; i < n; i++)
                nt[i] = new WebServerWorker();
            threads = nt;
            return;
        }
        System.arraycopy(ot, 0, nt, 0, n);
        threads = nt;
        for (int i = n; i < ot.length; i++) {
            ot[i].isAlive = false;
            ot[i].selector.wakeup();
        }
    }

    public class WebServerWorker extends Thread {
        public final Selector selector;
        public volatile boolean isAlive = true;
        public final ConcurrentLinkedQueue<SelectableChannel> channels = new ConcurrentLinkedQueue<>();

        public final BuffLand land = new BuffLand();

        public ByteBuffer bl1 = land.bl1();

        public WebServerWorker() throws IOException {
            this.selector = Selector.open();
            setName("WebServer Worker ");
            setPriority(Thread.MAX_PRIORITY);
            start();
        }

        public WebServer getServer() {
            return WebServer.this;
        }

        @Override
        public void run() {
            try {
                while (isAlive) {
                    selector.select();
                    for (final SelectionKey key : selector.selectedKeys())
                        try {
                            ((Task) key.attachment()).tick();
                        } catch (final Throwable ex) {
                            L.e(ex);
                            try {
                                ((Task) key.attachment()).recycle();
                            } catch (final Throwable ex1) {
                                L.e(ex1);
                                try {
                                    key.channel().close();
                                } catch (final IOException ignored) {}
                            }
                            key.cancel();
                            bl1.clear();
                        }
                    selector.selectedKeys().clear();
                    SelectableChannel sch;
                    //noinspection resource
                    while ((sch = channels.poll()) != null) {
                        final AcceptTask t = new AcceptTask();
                        t.setBase(this, sch.register(selector, SelectionKey.OP_READ, t));
                    }
                }
                for (final SelectionKey key : selector.keys()) {
                    key.cancel();
                    accept(key.channel());
                }
                SelectableChannel sch;
                while ((sch = channels.poll()) != null)
                    accept(sch);
                selector.close();
            } catch (final Throwable ex) {
                L.e(ex);
            }
        }
    }
}