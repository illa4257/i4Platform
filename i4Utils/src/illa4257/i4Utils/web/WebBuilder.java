package illa4257.i4Utils.web;

import illa4257.i4Utils.lists.MutableCharArray;
import illa4257.i4Utils.Str;
import illa4257.i4Utils.SyncVar;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WebBuilder {
    public static final List<String> METHODS = Arrays.asList("GET", "POST", "DELETE", "PUT");
    public static final List<String> PROTOCOLS = Arrays.asList("http", "https");
    private static boolean isEmpty(final String str) { return str == null || str.isEmpty(); }

    public final SyncVar<SocketFactory> sslSocketFactory = new SyncVar<>();
    public final AtomicInteger connectionTimeout = new AtomicInteger(5000), timeout = new AtomicInteger(5000), maxRedirects = new AtomicInteger(5);
    public final AtomicBoolean sendBaseHeaders = new AtomicBoolean(true), followRedirects = new AtomicBoolean(true);

    public final ConcurrentHashMap<String, Object> headers = new ConcurrentHashMap<>();

    public WebBuilder() { headers.put("User-Agent", "i4Web/1.0"); }

    public WebSocket open(final String method, final String uri) throws IOException { return open(method, new i4URI(uri), maxRedirects.get()); }
    public WebSocket open(final String method, final i4URI uri) throws IOException { return open(method, uri, maxRedirects.get()); }

    public WebSocket open(final String method, final i4URI uri, final int maxRedirects) throws IOException {
        return send(method, uri, null, maxRedirects);
    }

    public WebSocket send(final String method, final i4URI uri, final byte[] data) throws IOException {
        return send(method, uri, data, maxRedirects.get());
    }

    public WebSocket send(String method, final i4URI uri, final byte[] data, final int maxRedirects) throws IOException {
        if (isEmpty(method))
            throw new IllegalArgumentException("Method cannot be null or empty");
        method = method.toUpperCase();
        if (!METHODS.contains(method))
            throw new IllegalArgumentException("Unknown method");
        if (uri == null)
            throw new IllegalArgumentException("URI cannot be null");
        if (!PROTOCOLS.contains(uri.scheme))
            throw new IllegalArgumentException("Unknown protocol");


        final boolean isSecure = uri.scheme.equals("https");

        final Socket socket =
                isSecure ?
                        sslSocketFactory.get(SSLSocketFactory.getDefault()).createSocket() :
                        new Socket();

        final int p = uri.port < 0 ? isSecure ? 443 : 80 : uri.port;
        final int ct = connectionTimeout.get();
        if (ct < 1)
            socket.connect(new InetSocketAddress(uri.domain, p));
        else
            socket.connect(new InetSocketAddress(uri.domain, p), ct);

        final int t = timeout.get();
        if (t > 0)
            socket.setSoTimeout(t);

        final WebStream s = new WebStream(socket);

        s.write(method + ' ' + (uri.path == null ? '/' : Str.encodeURI(uri.path, false)) + " HTTP/1.1\r\n");

        boolean host = true, contentLength = true, connection = true;

        for (final Map.Entry<String, Object> e : headers.entrySet()) {
            final Object v = e.getValue();
            if (v == null)
                continue;
            final String k = e.getKey();
            switch (k) {
                case "Host":
                    host = false;
                    break;
                case "Content-Length":
                    contentLength = false;
                    break;
                case "Connection":
                    connection = false;
                    break;
            }
            s.write(k + ": " + (v instanceof MutableCharArray ? ((MutableCharArray) v).getChars() : v) + "\r\n");
        }

        if (sendBaseHeaders.get()) {
            if (host)
                s.write("Host: " + uri.domain + (uri.port < 0 || (isSecure && uri.port == 443) || (!isSecure && uri.port == 80) ? "" : ':' + uri.port) + "\r\n");
            if (contentLength)
                s.write("Content-Length: " + (data != null ? data.length : 0) + "\r\n");
            if (connection)
                s.write("Connection: closed\r\n");
        }

        s.write("\r\n");
        s.flush();

        if (data != null) {
            s.write(data);
            s.flush();
        }

        if (followRedirects.get()) {
            final WebSocket sock = new WebSocket(s, false);
            final String loc = sock.headers.get("location");
            if (sock.responseCode >= 300 && sock.responseCode < 400 && loc != null && !loc.isEmpty()) {
                sock.close();
                return open(method, new i4URI(loc), maxRedirects - 1);
            }
            return sock;
        }
        return new WebSocket(s, false);
    }
}