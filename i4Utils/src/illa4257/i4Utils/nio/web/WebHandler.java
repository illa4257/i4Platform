package illa4257.i4Utils.nio.web;

import illa4257.i4Utils.nio.web.tasks.WebTask;

public abstract class WebHandler extends WebTask {
    public WebProtocol protocol;

    public WebHandler(final WebProtocol protocol) {
        this.protocol = protocol;
    }

    public void header(@SuppressWarnings("unused") final String key, @SuppressWarnings("unused") final String value) {}
    public void header(@SuppressWarnings("unused") final String key) {}

    public WSHandler websocket(@SuppressWarnings("unused") final WSProtocol protocol) { return null; }
}