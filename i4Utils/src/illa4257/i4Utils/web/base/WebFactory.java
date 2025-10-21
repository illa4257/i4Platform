package illa4257.i4Utils.web.base;

import illa4257.i4Utils.Arch;
import illa4257.i4Utils.CloseableSyncVar;
import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.io.NullInputStream;
import illa4257.i4Utils.str.Str;
import illa4257.i4Utils.io.NullOutputStream;
import illa4257.i4Utils.logger.i4Logger;
import illa4257.i4Utils.runnables.FuncIOEx;
import illa4257.i4Utils.web.IWebClientFactory;
import illa4257.i4Utils.web.WebRequest;
import illa4257.i4Utils.web.i4URI;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static illa4257.i4Utils.logger.Level.WARN;

public class WebFactory implements IWebClientFactory {
    public static final ConcurrentHashMap<String, FuncIOEx<InputStream, InputStream>> DECOMPRESSORS = new ConcurrentHashMap<>();
    public static volatile String DECOMPRESSORS_VALUE;
    public static final Charset CHARSET = StandardCharsets.US_ASCII;
    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    });

    public static final WebFactory INSTANCE = new WebFactory();

    private static final ThreadLocal<Integer> oldByte = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<StringBuilder> strReadBuff = ThreadLocal.withInitial(StringBuilder::new);

    private final Scheduler scheduler = new Scheduler();

    private static final long SYSTEM_CHECK_DELAY = 5_000;

    private static final Object sys = new Object();
    private static volatile boolean sysCheck = false;
    private static long sysNextCheck = 0;
    private static volatile long sysKeepAliveWait = 0;

    public volatile SocketFactory sslFactory = SSLSocketFactory.getDefault();

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<SocketCon>>
            connections = new ConcurrentHashMap<>(),
            securedConnections = new ConcurrentHashMap<>();

    static {
        DECOMPRESSORS.put("gzip", GZIPInputStream::new);
        DECOMPRESSORS.put("deflate", InflaterInputStream::new);
        
        DECOMPRESSORS_VALUE = String.join(", ", DECOMPRESSORS.keySet());

        try {
            if (Arch.JVM.IS_LINUX) {
                final File
                        keepaliveTime = new File("/proc/sys/net/ipv4/tcp_keepalive_time"),
                        keepaliveProbes = new File("/proc/sys/net/ipv4/tcp_keepalive_probes"),
                        keepaliveIntVL = new File("/proc/sys/net/ipv4/tcp_keepalive_intvl");

                final List<String> nl = Arrays.asList(keepaliveTime.getName(), keepaliveProbes.getName(), keepaliveIntVL.getName());

                final WatchService s = FileSystems.getDefault().newWatchService();
                Paths.get("/proc/sys/net/ipv4/").register(s, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                new Thread() {
                    {
                        setName("Network Configuration monitor (Linux)");
                        setDaemon(true);
                    }

                    @Override
                    public void run() {
                        try {
                            boolean changed = true;
                            //noinspection InfiniteLoopStatement
                            while (true) {
                                if (changed) {
                                    changed = false;
                                    sysKeepAliveWait = (
                                            Long.parseLong(new String(IO.readFully(keepaliveTime), StandardCharsets.UTF_8).trim()) +
                                                    Long.parseLong(new String(IO.readFully(keepaliveIntVL), StandardCharsets.UTF_8).trim()) *
                                                (Long.parseLong(new String(IO.readFully(keepaliveProbes), StandardCharsets.UTF_8).trim()) - 1)
                                    ) * 1000;
                                }
                                final WatchKey k = s.take();
                                for (final WatchEvent<?> e : k.pollEvents())
                                    if (nl.contains(((Path) e.context()).toString())) {
                                        changed = true;
                                        break;
                                    }
                                k.reset();
                            }
                        } catch (final Exception ex) {
                            i4Logger.INSTANCE.log(ex);
                        }
                    }
                }.start();
            } else
                sysKeepAliveWait = (120 * 60 + 75 * (9 - 1)) * 1000;
        } catch (final Exception ex) {
            i4Logger.INSTANCE.log(ex);
        }
    }

    public static long getKeepAliveWait() {
        if (sysCheck && System.currentTimeMillis() > sysNextCheck && sysCheck)
            synchronized (sys) {
                if (System.currentTimeMillis() < sysNextCheck || !sysCheck)
                    return sysKeepAliveWait;
                try {
                    sysNextCheck = System.currentTimeMillis() + SYSTEM_CHECK_DELAY;
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(WARN, ex);
                    sysCheck = false;
                }
            }
        return sysKeepAliveWait;
    }

    private class Scheduler extends Thread {
        private final Object schedulerLocker = new Object();
        private long max = -1;

        public Scheduler() {
            setName("Web Scheduler");
            setDaemon(true);
            start();
        }

        public void schedule(final long nv) {
            synchronized (schedulerLocker) {
                if (nv < max)
                    return;
                max = nv;
                schedulerLocker.notify();
            }
        }

        @Override
        public void run() {
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    final long d;
                    synchronized (schedulerLocker) {
                        if (max == -1)
                            schedulerLocker.wait();
                        d = max;
                        max = -1;
                    }
                    //noinspection BusyWait
                    Thread.sleep(d);
                    reduce();
                }
            } catch (final InterruptedException ex) {
                i4Logger.INSTANCE.log(ex);
            }
        }
    }

    private static class SocketCon {
        public final Socket socket;
        public final long close;

        public SocketCon(final Socket s, final long close) {
            socket = s;
            this.close = close;
        }

        public boolean isClosed() {
            if (socket.isClosed())
                return true;
            if (close < System.currentTimeMillis()) {
                try {
                    socket.close();
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.log(ex);
                }
                return true;
            }
            return false;
        }
    }

    public WebFactory() {}

    public void reduce() {
        connections.values().removeIf(q -> {
            q.removeIf(SocketCon::isClosed);
            return q.isEmpty();
        });
        securedConnections.values().removeIf(q -> {
            q.removeIf(SocketCon::isClosed);
            return q.isEmpty();
        });
    }

    private Socket newCon1(final boolean isSecure, final String domain, final int port, final int timeout,
                           final String protocol, final byte[] initData) throws IOException {
        try (final CloseableSyncVar<Socket> var = new CloseableSyncVar<>(
                isSecure ? sslFactory.createSocket() : new Socket()
        )) {
            var.get().connect(new InetSocketAddress(domain, port), timeout);
            var.get().setSoTimeout(timeout);
            var.get().setKeepAlive(true);
            var.get().setSoLinger(true, Math.max(timeout / 1000, 1));

            var.get().getOutputStream().write(initData);
            var.preventClosing.set(true);
            return var.get();
        }
    }

    private Socket newCon0(final boolean isSecure, final String domain, final int port, final int timeout,
                           final String protocol, final byte[] initData) throws IOException {
        final ConcurrentHashMap<String, ConcurrentLinkedQueue<SocketCon>> m = isSecure ? securedConnections : connections;
        final ConcurrentLinkedQueue<SocketCon> q = m.get(domain + ':' + port);
        if (q == null)
            return newCon1(isSecure, domain, port, timeout, protocol, initData);
        while (true) {
            final SocketCon s = q.poll();
            if (s == null) {
                m.remove(domain + ':' + port, q);
                return newCon1(isSecure, domain, port, timeout, protocol, initData);
            }
            if (s.isClosed())
                continue;
            try {
                s.socket.setSoTimeout(timeout);
                final OutputStream o = s.socket.getOutputStream();
                o.write(initData[0]);
                o.flush();
                o.write(initData, 1, initData.length - 1);
            } catch (final SocketException ex) {
                if (
                        "Broken pipe".equalsIgnoreCase(ex.getMessage()) ||
                        "Broken pipe (Write failed)".equalsIgnoreCase(ex.getMessage())
                )
                    continue;
                i4Logger.INSTANCE.log(ex);
                continue;
            }
            return s.socket;
        }
    }

    protected void pass(final boolean isSecure, final String domain, final int port, final Socket socket, final long lastWrittenData) {
        final long d = getKeepAliveWait();
        if (d <= 0)
            return;
        (isSecure ? securedConnections : connections).computeIfAbsent(domain + ':' + port, k -> new ConcurrentLinkedQueue<>())
                .offer(new SocketCon(socket, lastWrittenData + d));
        scheduler.schedule(d);
    }

    private static int readByte(final InputStream is) throws IOException {
        final int r = oldByte.get();
        if (r != -1) {
            oldByte.set(-1);
            return r;
        }
        return IO.readByteI(is);
    }

    private static String readStr(final InputStream is, final int max) throws IOException {
        final StringBuilder b = strReadBuff.get();
        b.setLength(0);
        for (int i = 0; i < max; i++) {
            final char ch = (char) readByte(is);
            if (ch == ' ')
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

    private static String readStrLn(final InputStream is) throws IOException {
        final StringBuilder b = strReadBuff.get();
        b.setLength(0);
        boolean r = false;
        for (int i = 0; i < 256; i++) {
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
            if (ch == ':') {
                oldByte.set((int) ':');
                return b.toString();
            }
            b.append(ch);
        }
        throw new IOException("Reached the maximum number of characters.");
    }

    public static WebRequest accept(final InputStream is, final OutputStream os, final String ip, final String scheme, final Runnable end) throws IOException {
        oldByte.set(-1);
        final WebRequest r = new WebRequest()
                .setMethod(readStr(is, 8))
                .setURI(new i4URI(
                        scheme,
                        ip,
                        readStr(is, 256)
                ))
                .setProtocol(readStrLn(is, 16));
        r.isClient = false;
        r.outputStream = os;
        readHeaders(is, r.clientHeaders);

        r.inputStream = null;

        if ("upgrade".equalsIgnoreCase(WebRequest.getHeader(r.clientHeaders, "Connection")) && "websocket".equalsIgnoreCase(WebRequest.getHeader(r.clientHeaders, "Upgrade"))) {
            final String key = WebRequest.getHeader(r.clientHeaders, "Sec-WebSocket-Key");
            if (key != null && !key.isEmpty())
                r.setHeader("Sec-WebSocket-Accept", Base64.getEncoder().encodeToString(SHA1.get().digest(
                        (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(CHARSET)
                )));
            r.uri = new i4URI("https".equalsIgnoreCase(scheme) ? "wss" : "ws", ip, r.uri.fullPath);
            r.responseCode = 101;
            r.responseStatus = "Switching Protocols";
            r.setHeader("Upgrade", "websocket");
            r.setHeader("Connection", "Upgrade");
        } else {
            r.responseCode = 200;
            r.responseStatus = "OK";
            final String contentLength = WebRequest.getHeader(r.clientHeaders, "content-length");
            if (contentLength != null)
                try {
                    r.inputStream = new WebInputStream.LongPolling(is, end, Long.parseLong(contentLength));
                } catch (final Exception ex) {
                    i4Logger.INSTANCE.e(ex);
                }
            else if ("chunked".equalsIgnoreCase(WebRequest.getHeader(r.clientHeaders, "transfer-encoding")))
                r.inputStream = new WebInputStream.Chunked(is, end);
        }

        if (r.inputStream == null)
            r.inputStream = new NullInputStream();

        final String contentEncoding = WebRequest.getHeader(r.clientHeaders, "content-encoding");
        if (contentEncoding != null) {
            final FuncIOEx<InputStream, InputStream> decompressor = DECOMPRESSORS.get(contentEncoding);
            if (decompressor != null)
                r.inputStream = decompressor.accept(r.inputStream);
            else
                i4Logger.INSTANCE.w("Unknown content encoding method:", contentEncoding);
        }

        r.setRunner(req -> CompletableFuture.supplyAsync(() -> {
            final StringBuilder b = Str.builder();
            try {
                b.append(req.protocol).append(' ').append(r.responseCode).append(' ').append(r.responseStatus).append("\r\n");
                os.write(b.toString().getBytes(CHARSET));
                writeHeaders(os, req.serverHeaders);
                if (!WebRequest.hasHeader(req.serverHeaders, "Content-Length") &&
                        !WebRequest.hasHeader(req.serverHeaders, "Transfer-Encoding") && req.responseCode != 101) {
                    if (req.hasContent) {
                        if (req.bodyOutput != null) {
                            b.setLength(0);
                            b.append("Content-Length: ").append(req.bodyOutput.length).append("\r\n");
                            os.write(b.toString().getBytes(CHARSET));
                        } else
                            os.write("Transfer-Encoding: chunked\r\n".getBytes(CHARSET));
                    } else
                        os.write("Content-Length: 0\r\n".getBytes(CHARSET));
                }
                if (!WebRequest.hasHeader(req.serverHeaders, "Connection"))
                    os.write("Connection: closed\r\n".getBytes(CHARSET));
                os.write("\r\n".getBytes(CHARSET));
                if (req.responseCode == 101) {
                    if (req.uri.scheme.startsWith("ws")) {
                        final WSOutputStreamImpl wos = new WSOutputStreamImpl(os);
                        wos.masking = false;
                        req.outputStream = wos;
                        req.inputStream = new WSInputStreamImpl(is, d -> {
                            try {
                                wos.ping(d);
                            } catch (final Exception ex) {
                                i4Logger.INSTANCE.e(ex);
                            }
                        });
                    }
                } else if (req.hasContent)
                    if (req.bodyOutput == null)
                        req.outputStream = new WebOutputStream.Chunked(os, req);
                    else
                        os.write(req.bodyOutput);
                else
                    os.write("\r\n".getBytes(CHARSET));
                os.flush();
                return req;
            } catch (final Exception ex) {
                throw new CompletionException(ex);
            } finally {
                Str.recycle(b);
            }
        }));
        return r;
    }

    public static void writeHeaders(final OutputStream os, final Map<String, List<String>> headers) throws IOException {
        for (final Map.Entry<String, List<String>> e : headers.entrySet())
            for (final String v : e.getValue())
                os.write((e.getKey() + ": " + v + "\r\n").getBytes(CHARSET));
    }

    public static void readHeaders(final InputStream is, final Map<String, List<String>> headers) throws IOException {
        while (true) {
            final String k = readStrLn(is).trim();
            if (k.isEmpty() || oldByte.get() != ':')
                break;
            oldByte.set(-1);
            headers.computeIfAbsent(k, ignored -> new ArrayList<>()).add(readStrLn(is, 4090).trim());
        }
    }

    @Override
    public CompletableFuture<WebRequest> open(final WebRequest r) {
        return CompletableFuture.supplyAsync(() -> {
            final boolean isSecure = r.uri.scheme.equalsIgnoreCase("https") || r.uri.scheme.equalsIgnoreCase("wss");
            final int port = r.uri.port >= 0 ? r.uri.port : isSecure ? 443 : 80;
            try (final CloseableSyncVar<Socket> v = new CloseableSyncVar<>(newCon0(isSecure, r.uri.domain, port, r.timeout, r.protocol, r.method.getBytes(CHARSET)))) {
                oldByte.set(-1);
                final Socket s = v.get();
                final OutputStream os = s.getOutputStream();

                final String connectionHeader = WebRequest.getHeader(r.clientHeaders, "connection"),
                        upgradeHeader = WebRequest.getHeader(r.clientHeaders, "upgrade");
                os.write((" " + (r.uri.fullPath == null ? '/' : Str.encodeURI(r.uri.fullPath, false)) + ' ' + r.protocol + "\r\n").getBytes(CHARSET));

                if (!r.clientHeaders.containsKey("host"))
                    os.write(("Host: " + r.uri.domain + (
                            (isSecure && port == 443) ||
                                    (!isSecure && port == 80) ? "" : ":" + port) + "\r\n").getBytes(CHARSET));
                writeHeaders(os, r.clientHeaders);
                String upgrade = upgradeHeader == null && (
                                r.uri.scheme.equalsIgnoreCase("ws") ||
                                r.uri.scheme.equalsIgnoreCase("wss")
                        ) ? "websocket" : upgradeHeader;
                final boolean keepAlive = connectionHeader != null ? "keep-alive".equalsIgnoreCase(connectionHeader) : r.keepAlive,
                            isWebSocketClient = "websocket".equalsIgnoreCase(upgrade);
                final String dec = DECOMPRESSORS_VALUE;
                if (!r.clientHeaders.containsKey("accept-encoding") && dec != null && !dec.isEmpty())
                    os.write(("Accept-Encoding: " + dec + "\r\n").getBytes(CHARSET));
                if (r.hasContent) {
                    if (r.bodyOutput != null) {
                        if (!r.clientHeaders.containsKey("content-length"))
                            os.write(("Content-Length: " + r.bodyOutput.length + "\r\n").getBytes(CHARSET));
                    } else if (!r.clientHeaders.containsKey("transfer-encoding"))
                        os.write(("Transfer-Encoding: chunked\r\n").getBytes(CHARSET));
                } else if (!r.clientHeaders.containsKey("content-length"))
                    os.write(("Content-Length: 0\r\n").getBytes(CHARSET));
                if (upgradeHeader == null && upgrade != null)
                    os.write(("Upgrade: " + upgrade + "\r\n").getBytes(CHARSET));
                if (!r.clientHeaders.containsKey("connection"))
                    os.write(("Connection: " + (upgrade != null ? "upgrade" : keepAlive ? "keep-alive" : "closed") + "\r\n").getBytes(CHARSET));
                if (isWebSocketClient) {
                    if (!r.clientHeaders.containsKey("sec-websocket-version"))
                        os.write("Sec-WebSocket-Version: 13\r\n".getBytes(CHARSET));
                    if (r.clientHeaders.containsKey("sec-websocket-key"))
                        r.reserved = r.clientHeaders.get("sec-websocket-key");
                    else
                        os.write(("Sec-WebSocket-Key: " + (
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
                        r.outputStream = new WebOutputStream.Chunked(os, r);
                    else {
                        os.write(r.bodyOutput);
                        r.outputStream = new NullOutputStream();
                    }
                }
                os.flush();
                r.lastWrittenData = System.currentTimeMillis();
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
                r.protocol = readStr(is, 12);
                r.responseCode = Integer.parseInt(readStr(is, 4));
                r.responseStatus = readStrLn(is, 32);
                readHeaders(is, r.serverHeaders);

                final Runnable end = () -> {
                    if (s.isClosed())
                        return;
                    final String con = WebRequest.getHeader(r.serverHeaders, "connection");
                    if (con != null)
                        for (String a : con.split(",")) {
                            a = a.trim();
                            if (a.equalsIgnoreCase("keep-alive")) {
                                pass(isSecure, r.uri.domain, port, s, r.lastWrittenData);
                                return;
                            }
                        }
                    try {
                        s.close();
                    } catch (final IOException ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                };

                r.inputStream = null;
                final String contentLength = WebRequest.getHeader(r.serverHeaders, "content-length");
                if (contentLength != null)
                    try {
                        r.inputStream = new WebInputStream.LongPolling(is, end, Long.parseLong(contentLength));
                    } catch (final Exception ex) {
                        i4Logger.INSTANCE.log(ex);
                    }
                else if ("chunked".equalsIgnoreCase(WebRequest.getHeader(r.serverHeaders, "transfer-encoding")))
                    r.inputStream = new WebInputStream.Chunked(is, end);
                else if (r.responseCode == 101 && "upgrade".equalsIgnoreCase(WebRequest.getHeader(r.serverHeaders, "connection"))) {
                    final String upgrade = WebRequest.getHeader(r.serverHeaders, "upgrade");
                    if (upgrade != null) {
                        if ("websocket".equalsIgnoreCase(upgrade)) {
                            if (r.reserved instanceof String) {
                                if (!Base64.getEncoder()
                                        .encodeToString(SHA1.get().digest((r.reserved + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes(CHARSET)))
                                        .equals(WebRequest.getHeader(r.serverHeaders, "sec-websocket-accept"))) {
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


                final String contentEncoding = WebRequest.getHeader(r.serverHeaders, "content-encoding");
                if (contentEncoding != null && !contentEncoding.isEmpty()) {
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
