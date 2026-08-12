package illa4257.i4Utils.nio.web.tasks;

import illa4257.i4Utils.nio.net.NetServer;
import illa4257.i4Utils.nio.net.tasks.Task;
import illa4257.i4Utils.nio.web.WebServer;
import illa4257.i4Utils.nio.net.transports.RawTransport;

import java.nio.channels.SelectionKey;

public abstract class WebTask extends Task {
    public WebServer server = null;

    public Task setBase(final Task task) {
        super.setBase(task);
        this.server = (WebServer) task.server;
        return this;
    }

    public Task setBase(final NetServer.NetServerWorker worker, final SelectionKey key) {
        super.setBase(worker, key);
        this.server = (WebServer) worker.getServer();
        return this;
    }
}