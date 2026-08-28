package illa4257.i4Utils.nio.web;

import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.nio.net.NetServer;
import illa4257.i4Utils.nio.web.tasks.WebAcceptTask;
import illa4257.i4Utils.nio.net.tasks.Task;

import javax.net.ssl.SSLEngine;
import java.nio.channels.SelectableChannel;

public abstract class WebServer extends NetServer {
    public static final i4Logger L = new i4Logger("WebServer").registerHandler(i4Logger.INSTANCE);

    public abstract SSLEngine createEngine();
    public abstract WebHandler getHandler(final String method, final String path, final String protocol, final WebProtocol p);

    @Override
    public Task getHandler(final SelectableChannel channel) {
        return new WebAcceptTask();
    }
}