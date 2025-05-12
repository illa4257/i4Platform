package illa4257.i4Utils.web;

import illa4257.i4Utils.CloseableSyncVar;
import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.io.NullInputStream;
import illa4257.i4Utils.Str;
import illa4257.i4Utils.io.NullOutputStream;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.runnables.FuncIOEx;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static illa4257.i4Utils.logger.Level.WARN;

public class WebClientFactory implements IWebClientFactory {
    public static final ConcurrentHashMap<String, FuncIOEx<InputStream, InputStream>> DECOMPRESSORS = new ConcurrentHashMap<>();
    public static volatile String DECOMPRESSORS_VALUE;
    public static final Charset CHARSET = StandardCharsets.US_ASCII;

    static {
        DECOMPRESSORS.put("gzip", GZIPInputStream::new);
        DECOMPRESSORS.put("deflate", InflaterInputStream::new);
        
        DECOMPRESSORS_VALUE = String.join(", ", DECOMPRESSORS.keySet());
    }

    public volatile SocketFactory sslFactory = SSLSocketFactory.getDefault();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Socket>> connections = new ConcurrentHashMap<>(), securedConnections = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> oldByte = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<StringBuilder> strReadBuff = ThreadLocal.withInitial(StringBuilder::new);

    public WebClientFactory() {}

    private void reduce() {
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
            final boolean isSecure = r.uri.scheme.equalsIgnoreCase("https");
            final int port = r.uri.port >= 0 ? r.uri.port : isSecure ? 443 : 80;
            try (final CloseableSyncVar<Socket> v = new CloseableSyncVar<>(newCon0(isSecure, r.uri.domain, port, r.timeout))) {
                oldByte.set(-1);
                final Socket s = v.get();
                s.setSoTimeout(r.timeout);
                s.setKeepAlive(true);
                final OutputStream os = s.getOutputStream();

                final String connectionHeader = r.clientHeaders.get("Connection");
                os.write((r.method + ' ' + (r.uri.fullPath == null ? '/' : Str.encodeURI(r.uri.fullPath, false)) + ' ' + r.protocol + "\r\n").getBytes(CHARSET));

                if (!r.clientHeaders.containsKey("Host"))
                    os.write(("Host: " + r.uri.domain + (
                            (isSecure && port == 443) ||
                                    (!isSecure && port == 80) ? "" : ":" + port) + "\r\n").getBytes(CHARSET));
                writeHeaders(os, r.clientHeaders);
                final boolean keepAlive = connectionHeader != null ? "keep-alive".equalsIgnoreCase(connectionHeader) : r.keepAlive;

                final String dec = DECOMPRESSORS_VALUE;
                if (!r.clientHeaders.containsKey("Accept-Encoding") && dec != null && !dec.isEmpty())
                    os.write(("Accept-Encoding: " + dec + "\r\n").getBytes(CHARSET));
                if (r.hasContent) {
                    if (r.bodyOutput != null) {
                        if (!r.clientHeaders.containsKey("Content-Length"))
                            os.write(("Content-Length: " + r.bodyOutput.length + "\r\n").getBytes(CHARSET));
                    } else if (!r.clientHeaders.containsKey("Transfer-Encoding")) {
                        System.out.println("HEADER CHUNKED");
                        os.write(("Transfer-Encoding: chunked\r\n").getBytes(CHARSET));
                    }
                } else if (!r.clientHeaders.containsKey("Content-Length"))
                    os.write(("Content-Length: 0\r\n").getBytes(CHARSET));
                if (r.clientHeaders.containsKey("Connection"))
                    os.write(("Connection: " + (keepAlive ? "keep-alive" : "closed") + "\r\n").getBytes(CHARSET));
                os.write("\r\n".getBytes(CHARSET));
                if (!r.hasContent || r.bodyOutput == null)
                    r.outputStream = new WebOutputStream.Chunked(os);
                else {
                    os.write(r.bodyOutput);
                    r.outputStream = new NullOutputStream();
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
                    if ("keep-alive".equalsIgnoreCase(r.serverHeaders.get("Connection")))
                        pass(isSecure, r.uri.domain, port, s);
                };

                r.inputStream = null;
                final String contentLength = r.serverHeaders.get("Content-Length");
                if (contentLength != null)
                    try {
                        r.inputStream = new WebInputStream.LongPolling(is, end, Long.parseLong(contentLength));
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                else if ("chunked".equalsIgnoreCase(r.serverHeaders.get("Transfer-Encoding")))
                    r.inputStream = new WebInputStream.Chunked(is, end);

                if (r.inputStream == null) {
                    r.inputStream = new NullInputStream();
                    return r;
                }

                final String contentEncoding = r.serverHeaders.get("Content-Encoding");
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
