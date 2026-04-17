package illa4257.i4Utils.nio.web;

import illa4257.i4Utils.nio.web.tasks.Protocol;
import illa4257.i4Utils.nio.web.tasks.Task;

public abstract class WebHandler extends Task {
    public Protocol protocol;

    public WebHandler(final Protocol protocol) {
        this.protocol = protocol;
    }

    public void header(final String key, final String value) {}
    public void header(final String key) {}
}