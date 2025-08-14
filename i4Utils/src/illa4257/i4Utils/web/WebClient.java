package illa4257.i4Utils.web;

import illa4257.i4Utils.Arch;
import illa4257.i4Utils.PreservedKeyMap;
import illa4257.i4Utils.web.base.WebFactory;
import illa4257.i4Utils.web.cheerpj.CheerpJClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebClient {
    public final IWebClientFactory factory;
    public final Map<String, List<String>> headers = new PreservedKeyMap<>(
            new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), String::toLowerCase,
            k -> k instanceof String ? ((String) k).toLowerCase() : k
    );

    public volatile boolean keepAlive = true;
    public volatile int timeout = 15000;

    public WebClient(final IWebClientFactory factory) { this.factory = factory; }
    public WebClient() { this(Arch.REAL.IS_CHEERPJ ? new CheerpJClientFactory() : WebFactory.INSTANCE); }

    public WebClient setHeader(final String key, final String value) {
        final ArrayList<String> l = new ArrayList<>();
        l.add(value);
        headers.put(key, l);
        return this;
    }

    public WebRequest newBuilder(final String method, final i4URI uri) {
        return new WebRequest(method, uri, this)
                .setRunner(factory::open);
    }

    public WebRequest newBuilder(final String method, final String uri) {
        return new WebRequest(method, new i4URI(uri), this)
                .setRunner(factory::open);
    }

    public CompletableFuture<WebRequest> open(final WebRequest builder) { return factory.open(builder); }
}