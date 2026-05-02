package illa4257.i4Utils.nio.web;

import illa4257.i4Utils.nio.web.tasks.Task;

public abstract class WSHandler extends Task {
    public WSProtocol protocol;

    public WSHandler(final WSProtocol protocol) {
        this.protocol = protocol;
    }

    public void open() {}
}