package illa4257.i4Utils.web;

import illa4257.i4Utils.KeyMap;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class WebRequest {
    public Object reserved = null;
    public boolean keepAlive = true, hasContent = false, isClient = true;
    public int responseCode = -1, timeout = 15000;
    public volatile long lastWrittenData = 0;
    public String protocol = "HTTP/1.1", responseStatus = null, method;
    public i4URI uri;
    public final Map<String, String> clientHeaders, serverHeaders = new KeyMap<>(
            new HashMap<>(), String::toLowerCase,
            k -> k instanceof String ? ((String) k).toLowerCase() : k
    );
    public byte[] bodyOutput = null;
    public OutputStream outputStream = null;
    public InputStream inputStream = null;
    public Function<WebRequest, CompletableFuture<WebRequest>> runner = null;

    public WebRequest() {
        clientHeaders = new KeyMap<>(new HashMap<>(), String::toLowerCase,
            k -> k instanceof String ? ((String) k).toLowerCase() : k);
    }

    public WebRequest(final String method, final i4URI uri) {
        this.method = method != null ? method : "GET";
        this.uri = uri;
        clientHeaders = new KeyMap<>(
                new HashMap<>(), String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k
        );
    }

    public WebRequest(final String method, final i4URI uri, final Map<String, String> headers) {
        this.method = method != null ? method : "GET";
        this.uri = uri;
        this.clientHeaders = new KeyMap<>(
                new HashMap<>(), headers, String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k
        );
    }

    public WebRequest(final String method, final i4URI uri, final WebClient client) {
        this.keepAlive = client.keepAlive;
        this.timeout = client.timeout;
        this.method = method != null ? method : "GET";
        this.uri = uri;
        this.clientHeaders = new KeyMap<>(
                new HashMap<>(client.headers), String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k
        );
    }

    public WebRequest setMethod(final String method) {
        this.method = method != null ? method : "GET";
        return this;
    }

    public WebRequest setProtocol(final String protocol) {
        this.protocol = protocol;
        return this;
    }

    public WebRequest setURI(final i4URI uri) {
        if (uri == null)
            return this;
        this.uri = uri;
        return this;
    }

    public WebRequest setHeader(final String key, final String value) {
        (isClient ? clientHeaders : serverHeaders).put(key, value);
        return this;
    }

    public WebRequest setHeaders(final Map<String, String> headers) {
        (isClient ? clientHeaders : serverHeaders).putAll(headers);
        return this;
    }

    public WebRequest setKeepAlive(final boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public WebRequest setTimeout(final int timeout) {
        this.timeout = timeout;
        return this;
    }

    public WebRequest setBody(final byte[] bodyOutput) {
        hasContent = true;
        this.bodyOutput = bodyOutput;
        return this;
    }

    public WebRequest chunked() {
        hasContent = true;
        bodyOutput = null;
        return this;
    }

    public WebRequest setRunner(final Function<WebRequest, CompletableFuture<WebRequest>> runner) {
        this.runner = runner;
        return this;
    }

    public CompletableFuture<WebRequest> run() {
        if (runner == null)
            return CompletableFuture.supplyAsync(() -> this);
        final Function<WebRequest, CompletableFuture<WebRequest>> s = runner;
        runner = null;
        return s.apply(this);
    }

    public byte[] getBodyOutput() { return bodyOutput; }
    public OutputStream getOutputStream() { return outputStream; }
    public InputStream getInputStream() { return inputStream; }
}