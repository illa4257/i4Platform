package illa4257.i4Utils.web;

import illa4257.i4Utils.PreservedKeyMap;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public final PreservedKeyMap<String, List<String>> clientHeaders, serverHeaders = new PreservedKeyMap<>(
            new HashMap<>(), new HashMap<>(), String::toLowerCase,
            k -> k instanceof String ? ((String) k).toLowerCase() : k
    );
    public byte[] bodyOutput = null;
    public OutputStream outputStream = null;
    public InputStream inputStream = null;
    public Function<WebRequest, CompletableFuture<WebRequest>> runner = null;

    public WebRequest() {
        clientHeaders = new PreservedKeyMap<>(new HashMap<>(), new HashMap<>(), String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k);
    }

    public WebRequest(final String method, final i4URI uri) {
        this.method = method != null ? method : "GET";
        this.uri = uri;
        clientHeaders = new PreservedKeyMap<>(
                new HashMap<>(), new HashMap<>(), String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k
        );
    }

    public WebRequest(final String method, final i4URI uri, final Map<String, List<String>> headers) {
        this.method = method != null ? method : "GET";
        this.uri = uri;
        this.clientHeaders = new PreservedKeyMap<>(
                new HashMap<>(), new HashMap<>(headers), String::toLowerCase,
                k -> k instanceof String ? ((String) k).toLowerCase() : k
        );
    }

    public WebRequest(final String method, final i4URI uri, final WebClient client) {
        this.keepAlive = client.keepAlive;
        this.timeout = client.timeout;
        this.method = method != null ? method : "GET";
        this.uri = uri;
        this.clientHeaders = new PreservedKeyMap<>(
                new HashMap<>(), new HashMap<>(client.headers), String::toLowerCase,
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
        setHeader(isClient ? clientHeaders : serverHeaders, key, value);
        return this;
    }

    public WebRequest setHeaders(final Map<String, List<String>> headers) {
        (isClient ? clientHeaders : serverHeaders).putAll(headers);
        return this;
    }

    public static void setHeader(final Map<String, List<String>> headers, final String key, final String value) {
        final List<String> l = headers.computeIfAbsent(key, ignored -> new ArrayList<>());
        if (!l.isEmpty())
            l.clear();
        l.add(value);
    }

    public static String getHeader(final Map<String, List<String>> headers, final String key) {
        final List<String> l = headers.get(key);
        if (l == null)
            return null;
        return l.isEmpty() ? null : l.get(0);
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
            return CompletableFuture.completedFuture(this);
        final Function<WebRequest, CompletableFuture<WebRequest>> s = runner;
        runner = null;
        return s.apply(this);
    }

    public byte[] getBodyOutput() { return bodyOutput; }
    public OutputStream getOutputStream() { return outputStream; }
    public InputStream getInputStream() { return inputStream; }
}