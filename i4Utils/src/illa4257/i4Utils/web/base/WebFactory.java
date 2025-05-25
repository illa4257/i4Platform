package illa4257.i4Utils.web.base;

import illa4257.i4Utils.CloseableSyncVar;
import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.io.NullInputStream;
import illa4257.i4Utils.Str;
import illa4257.i4Utils.io.NullOutputStream;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.runnables.FuncIOEx;
import illa4257.i4Utils.web.IWebClientFactory;
import illa4257.i4Utils.web.WebRequest;
import illa4257.i4Utils.web.i4URI;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static illa4257.i4Utils.logger.Level.WARN;

public class WebFactory implements IWebClientFactory {
    public static final ConcurrentHashMap<String, FuncIOEx<InputStream, InputStream>> DECOMPRESSORS = new ConcurrentHashMap<>();
    public static volatile String DECOMPRESSORS_VALUE;
    public static final Charset CHARSET = StandardCharsets.US_ASCII;
    private static final MessageDigest SHA1;

    public static final WebFactory INSTANCE = new WebFactory();

    static {
        DECOMPRESSORS.put("gzip", GZIPInputStream::new);
        DECOMPRESSORS.put("deflate", InflaterInputStream::new);
        
        DECOMPRESSORS_VALUE = String.join(", ", DECOMPRESSORS.keySet());

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (final Exception ex) {
            i4Logger.INSTANCE.log(ex);
        }
        SHA1 = digest;
    }

    public volatile SocketFactory sslFactory = SSLSocketFactory.getDefault();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Socket>> connections = new ConcurrentHashMap<>(), securedConnections = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> oldByte = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<StringBuilder> strReadBuff = ThreadLocal.withInitial(StringBuilder::new);

    public WebFactory() {}

    public void reduce() {
        connections.values().removeIf(q -> {
            q.removeIf(Socket::isClosed);
            return q.isEmpty();
        });
        securedConnections.values().removeIf(q -> {
            q.removeIf(Socket::isClosed);
            return q.isEmpty();
        });
    }

    private Socket newCon1(final boolean isSecure, final String domain, final int port, final int timeout) throws IOException {
        try (final CloseableSyncVar<Socket> var = new CloseableSyncVar<>(
                isSecure ? sslFactory.createSocket() : new Socket()
        )) {
            var.get().connect(new InetSocketAddress(domain, port), timeout);
            var.preventClosing.set(true);
            return var.get();
        }
    }

    private Socket newCon0(final boolean isSecure, final String domain, final int port, final int timeout) throws IOException {
        final ConcurrentHashMap<String, ConcurrentLinkedQueue<Socket>> m = isSecure ? securedConnections : connections;
        final ConcurrentLinkedQueue<Socket> q = m.get(domain + ':' + port);
        if (q == null)
            return newCon1(isSecure, domain, port, timeout);
        while (true) {
            final Socket s = q.poll();
            if (s == null) {
                m.remove(domain + ':' + port, q);
                return newCon1(isSecure, domain, port, timeout);
            }
            if (s.isClosed())
                continue;
            return s;
        }
    }

    protected void pass(final boolean isSecure, final String domain, final int port, final Socket socket) {
        (isSecure ? securedConnections : connections).computeIfAbsent(domain + ':' + port, k -> new ConcurrentLinkedQueue<>())
                .offer(socket);
    }

    private static int readByte(final InputStream is) throws IOException {
        final int r = oldByte.get();
        if (r != -1) {
            oldByte.set(-1);
            return r;
        }
        return IO.readByteI(is);
    }

    private static String readStr(final InputStream is, final char end, final int max) throws IOException {
        final StringBuilder b = strReadBuff.get();
        b.setLength(0);
        for (int i = 0; i < max; i++) {
            final char ch = (char) readByte(is);
            if (ch == end)
                return b.toString();
            b.append(ch);
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    private static String readStrLn(final InputStream is, final int max) throws IOException {
        final StringBuilder b = strReadBuff.get();
        b.setLength(0);
        boolean r = false;
        for (int i = 0; i < max; i++) {
            final char ch = (char) readByte(is);
            if (ch == '\r') {
                if (r) {
                    oldByte.set((int) '\r');
                    return b.toString();
                }
                r = true;
                continue;
            }
            if (ch == '\n')
                return b.toString();
            if (r) {
                oldByte.set((int) ch);
                return b.toString();
            }
            b.append(ch);
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    private static String readStrLn(final InputStream is, final char end, final int max) throws IOException {
        final StringBuilder b = strReadBuff.get();
        b.setLength(0);
        boolean r = false;
        for (int i = 0; i < max; i++) {
            final char ch = (char) readByte(is);
            if (ch == '\r') {
                if (r) {
                    oldByte.set((int) '\r');
                    return b.toString();
                }
                r = true;
                continue;
            }
            if (ch == '\n')
                return b.toString();
            if (r) {
                oldByte.set((int) ch);
                return b.toString();
            }
            if (ch == end) {
                oldByte.set((int) end);
                return b.toString();
            }
            b.append(ch);
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    public static WebRequest accept(final InputStream is, final String ip, final String scheme, final Runnable end) throws IOException {
        oldByte.set(-1);
        final WebRequest r = new WebRequest()
                .setMethod(readStr(is, ' ', 8))
                .setURI(new i4URI(
                        scheme,
                        ip,
                        readStr(is, ' ', 256)
                ))
                .setProtocol(readStrLn(is, 16));
        readHeaders(is, r.clientHeaders);

        r.inputStream = null;

        final String contentLength = r.clientHeaders.get("content-length");
        if (contentLength != null)
            try {
                r.inputStream = new WebInputStream.LongPolling(is, end, Long.parseLong(contentLength));
            } catch (final Exception ex) {
                i4Logger.INSTANCE.log(ex);
            }
        else if ("chunked".equalsIgnoreCase(r.clientHeaders.get("transfer-encoding")))
            r.inputStream = new WebInputStream.Chunked(is, end);

        if (r.inputStream == null) {
            r.inputStream = new NullInputStream();
            return r;
        }

        final String contentEncoding = r.clientHeaders.get("content-encoding");
        if (contentEncoding != null) {
            final FuncIOEx<InputStream, InputStream> decompressor = DECOMPRESSORS.get(contentEncoding);
            if (decompressor != null)
                r.inputStream = decompressor.accept(r.inputStream);
            else
                i4Logger.INSTANCE.log(WARN, "Unknown content encoding method: " + contentEncoding);
        }
        return r;
    }

    public static void writeHeaders(final OutputStream os, final Map<String, String> headers) throws IOException {
        for (final Map.Entry<String, String> e : headers.entrySet())
            os.write((e.getKey() + ": " + e.getValue() + "\r\n").getBytes(CHARSET));
    }

    public static void readHeaders(final InputStream is, final Map<String, String> headers) throws IOException {
        while (true) {
            final String k = readStrLn(is, ':', 64).trim();
            if (k.isEmpty() || oldByte.get() != ':')
                break;
            oldByte.set(-1);
            headers.put(k, readStrLn(is, 4096).trim());
        }
    }

    @Override
    public CompletableFuture<WebRequest> open(final WebRequest r) {
        return CompletableFuture.supplyAsync(() -> {
            final boolean isSecure = r.uri.scheme.equalsIgnoreCase("https") || r.uri.scheme.equalsIgnoreCase("wss");
            final int port = r.uri.port >= 0 ? r.uri.port : isSecure ? 443 : 80;
            try (final CloseableSyncVar<Socket> v = new CloseableSyncVar<>(newCon0(isSecure, r.uri.domain, port, r.timeout))) {
                oldByte.set(-1);
                final Socket s = v.get();
                s.setSoTimeout(r.timeout);
                s.setKeepAlive(true);
                final OutputStream os = s.getOutputStream();

                final String connectionHeader = r.clientHeaders.get("connection"), upgradeHeader = r.clientHeaders.get("upgrade");
                os.write((r.method + ' ' + (r.uri.fullPath == null ? '/' : Str.encodeURI(r.uri.fullPath, false)) + ' ' + r.protocol + "\r\n").getBytes(CHARSET));

                if (!r.clientHeaders.containsKey("host"))
                    os.write(("host: " + r.uri.domain + (
                            (isSecure && port == 443) ||
                                    (!isSecure && port == 80) ? "" : ":" + port) + "\r\n").getBytes(CHARSET));
                writeHeaders(os, r.clientHeaders);
                String upgrade = upgradeHeader == null && (
                                r.uri.scheme.equalsIgnoreCase("ws") ||
                                r.uri.scheme.equalsIgnoreCase("wss")
                        ) ? "websocket" : upgradeHeader, key = null;
                final boolean keepAlive = connectionHeader != null ? "keep-alive".equalsIgnoreCase(connectionHeader) : r.keepAlive,
                            isWebSocketClient = "websocket".equalsIgnoreCase(upgrade);
                final String dec = DECOMPRESSORS_VALUE;
                if (!r.clientHeaders.containsKey("accept-encoding") && dec != null && !dec.isEmpty())
                    os.write(("accept-encoding: " + dec + "\r\n").getBytes(CHARSET));
                if (r.hasContent) {
                    if (r.bodyOutput != null) {
                        if (!r.clientHeaders.containsKey("content-length"))
                            os.write(("content-length: " + r.bodyOutput.length + "\r\n").getBytes(CHARSET));
                    } else if (!r.clientHeaders.containsKey("transfer-encoding"))
                        os.write(("transfer-encoding: chunked\r\n").getBytes(CHARSET));
                } else if (!r.clientHeaders.containsKey("content-length"))
                    os.write(("content-length: 0\r\n").getBytes(CHARSET));
                if (upgradeHeader == null && upgrade != null)
                    os.write(("upgrade: " + upgrade + "\r\n").getBytes(CHARSET));
                if (!r.clientHeaders.containsKey("connection"))
                    os.write(("connection: " + (upgrade != null ? "upgrade" : keepAlive ? "keep-alive" : "closed") + "\r\n").getBytes(CHARSET));
                if (isWebSocketClient) {
                    if (!r.clientHeaders.containsKey("sec-websocket-version"))
                        os.write("sec-websocket-version: 13\r\n".getBytes(CHARSET));
                    if (r.clientHeaders.containsKey("sec-websocket-key"))
                        r.reserved = r.clientHeaders.get("sec-websocket-key");
                    else
                        os.write(("sec-websocket-key: " + (
                                r.reserved = Base64.getEncoder().encodeToString(Str.random(
                                        new SecureRandom(), 16, Str.STR_NUMS + Str.STR_EN_LOW + Str.STR_EN_UP
                                ).getBytes(CHARSET))
                        ) + "\r\n").getBytes(CHARSET));
                }
                os.write("\r\n".getBytes(CHARSET));
                if (isWebSocketClient)
                    r.outputStream = new WSOutputStreamImpl(os);
                else {
                    if (!r.hasContent || r.bodyOutput == null)
                        r.outputStream = new WebOutputStream.Chunked(os);
                    else {
                        os.write(r.bodyOutput);
                        r.outputStream = new NullOutputStream();
                    }
                }
                os.flush();
                v.preventClosing.set(true);
                r.setRunner(ignored -> processRead(s, r, port, isSecure));
                return r;
            } catch (final Exception ex) {
                throw new CompletionException(ex);
            }
        });
    }

    public CompletableFuture<WebRequest> processRead(final Socket s, final WebRequest r, final int port, final boolean isSecure) {
        return CompletableFuture.supplyAsync(() -> {
            try (final CloseableSyncVar<Socket> v2 = new CloseableSyncVar<>(s)) {
                final InputStream is = s.getInputStream();
                r.protocol = readStr(is, ' ', 12);
                r.responseCode = Integer.parseInt(readStr(is, ' ', 4));
                r.responseStatus = readStrLn(is, 32);
                readHeaders(is, r.serverHeaders);

                final Runnable end = () -> {
                    if (s.isClosed())
                        return;
                    if ("keep-alive".equalsIgnoreCase(r.serverHeaders.get("connection")))
                        pass(isSecure, r.uri.domain, port, s);
                    else
                        try {
                            s.close();
                        } catch (final IOException ex) {
                            i4Logger.INSTANCE.log(ex);
                        }
                };

                r.inputStream = null;
                final String contentLength = r.serverHeaders.get("content-length");
                if (contentLength != null)
                    try {
                        r.inputStream = new WebInputStream.LongPolling(is, end, Long.parseLong(contentLength));
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                else if ("chunked".equalsIgnoreCase(r.serverHeaders.get("transfer-encoding")))
                    r.inputStream = new WebInputStream.Chunked(is, end);
                else if (r.responseCode == 101 && "upgrade".equalsIgnoreCase(r.serverHeaders.get("connection"))) {
                    final String upgrade = r.serverHeaders.get("upgrade");
                    if (upgrade != null) {
                        if ("websocket".equalsIgnoreCase(upgrade)) {
                            if (r.reserved instanceof String) {
                                if (!Base64.getEncoder()
                                        .encodeToString(SHA1.digest((r.reserved + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes(StandardCharsets.US_ASCII)))
                                        .equals(r.serverHeaders.get("sec-websocket-accept"))) {
                                    r.reserved = null;
                                    throw new IOException("Failed validating");
                                }
                                r.reserved = null;
                            }
                            final OutputStream os = r.outputStream;
                            r.inputStream = new WSInputStreamImpl(is, os instanceof WSOutputStreamImpl ? (arr) -> {
                                try {
                                    ((WSOutputStreamImpl) os).pong(arr);
                                } catch (final Exception ex) {
                                    i4Logger.INSTANCE.log(ex);
                                }
                            } : null);
                        }
                    }
                }

                if (r.inputStream == null) {
                    r.inputStream = new NullInputStream();
                    return r;
                }

                final String contentEncoding = r.serverHeaders.get("content-encoding");
                if (contentEncoding != null) {
                    final FuncIOEx<InputStream, InputStream> decompressor = DECOMPRESSORS.get(contentEncoding);
                    if (decompressor != null)
                        r.inputStream = decompressor.accept(r.inputStream);
                    else
                        i4Logger.INSTANCE.log(WARN, "Unknown content encoding method: " + contentEncoding);
                }
                v2.preventClosing.set(true);
                return r;
            } catch (final Exception ex) {
                throw new CompletionException(ex);
            }
        });
    }
}
