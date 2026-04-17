package illa4257.i4Utils.nio.web.tasks;

import illa4257.i4Utils.nio.web.WebServer;
import illa4257.i4Utils.nio.web.transports.RawTransport;
import illa4257.i4Utils.nio.web.transports.Transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static illa4257.i4Utils.nio.web.WebServer.L;

public abstract class Task {
    public WebServer server = null;
    public WebServer.WebServerWorker worker = null;
    public Transport transport = null;

    public Task setBase(final Task task) {
        this.server = task.server;
        this.worker = task.worker;
        this.transport = task.transport;
        return this;
    }

    public Task setBase(final WebServer.WebServerWorker worker, final SelectionKey key) {
        this.server = worker.getServer();
        this.worker = worker;
        this.transport = new RawTransport(key);
        return this;
    }

    public int read(final ByteBuffer buffer) throws IOException {
        return transport.read(buffer);
    }

    public QTask getLast() {
        Task t = (Task) transport.attachment();
        if (t instanceof QTask) {
            while (((QTask) t).next instanceof QTask)
                t = ((QTask) t).next;
            return (QTask) t;
        }
        return null;
    }

    public void queue(final QTask task, final int interest) {
        task.setBase(this);
        final QTask last = getLast();
        if (last == null) {
            task.setQ((Task) transport.attachment(), transport.interestOps());
            transport.interestOps(interest);
            transport.attach(task);
        } else {
            task.setQ(last.next, last.interestOps);
            last.setQ(task, interest);
        }
    }

    public boolean write(final ByteBuffer buffer, final boolean recycle) throws IOException {
        final QTask last = getLast();
        if (last == null) {
            transport.write(buffer);
            if (!buffer.hasRemaining()) {
                if (recycle)
                    worker.land.recycle(buffer);
                return false;
            }
        }
        queue(new WriteTask().set(buffer, recycle), SelectionKey.OP_WRITE);
        return true;
    }

    public boolean copyReply(final ByteBuffer buffer, final long amount) throws IOException {
        System.out.println("Copy " + amount);
        if (amount == 0)
            return false;
        buffer.compact();
        queue(new CopyReplyTask(buffer, amount), SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        return true;
    }

    public abstract void tick() throws Exception;

    public void run(final Runnable runnable) {
        final CompletableTask task = new CompletableTask();
        queue(task, 0);
        server.runTask(() -> {
            try {
                runnable.run();
            } catch (final Throwable ex) {
                L.e(ex);
            }
            task.complete();
        });
    }

    public boolean isCurrent() {
        return transport.getSelectionKey().attachment() == this;
    }

    public void recycle() {
        try {
            transport.close();
        } catch (final Throwable ignored) {}
    }
}