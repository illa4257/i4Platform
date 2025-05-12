package illa4257.i4Utils.web;

import illa4257.i4Utils.Arch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebClient {
    private final IWebClientFactory factory;
    public final ConcurrentHashMap<String, String> headers = new ConcurrentHashMap<>();

    public volatile boolean keepAlive = true;
    public volatile int timeout = 15000;

    public WebClient(final IWebClientFactory factory) { this.factory = factory; }
    public WebClient() { this(Arch.REAL.IS_CHEERPJ ? new CheerpJClientFactory() : new WebClientFactory()); }

    public WebRequest newBuilder(final String method, final i4URI uri) {
        return new WebRequest(method, uri, this)
                .setRunner(factory::open);
    }

    public CompletableFuture<WebRequest> open(final WebRequest builder) {
        return factory.open(builder);
    }
}