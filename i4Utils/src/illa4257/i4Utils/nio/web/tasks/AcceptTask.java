package illa4257.i4Utils.nio.web.tasks;

import illa4257.i4Utils.nio.web.WebHandler;
import illa4257.i4Utils.nio.web.transports.SSLTransport;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static illa4257.i4Utils.nio.web.WebServer.L;

public class AcceptTask extends Task implements Protocol {
    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    });

    private static final byte[]
            LN = "\r\n".getBytes(StandardCharsets.US_ASCII),
            HEADER_SET = ": ".getBytes(StandardCharsets.US_ASCII),
            CLOSED = "Connection: close".getBytes(StandardCharsets.US_ASCII),
            CON_UPGRADE = "Connection: Upgrade".getBytes(StandardCharsets.US_ASCII),
            KEEP_ALIVE = "Connection: keep-alive".getBytes(StandardCharsets.US_ASCII),
            CONTENT_LENGTH = "Content-Length: ".getBytes(StandardCharsets.US_ASCII),
            NO_DATA = "Content-Length: 0".getBytes(StandardCharsets.US_ASCII),

            HEADER_UPGRADE_WS = "Upgrade: websocket".getBytes(StandardCharsets.US_ASCII),
            HEADER_WS_ACCEPT = "Sec-WebSocket-Accept: ".getBytes(StandardCharsets.US_ASCII),
            HEADER_WS_VER = "Sec-WebSocket-Version: 13".getBytes(StandardCharsets.US_ASCII),

            HTTP1_0 = "HTTP/1.0 ".getBytes(StandardCharsets.US_ASCII),
            HTTP1_1 = "HTTP/1.1 ".getBytes(StandardCharsets.US_ASCII);

    private static final byte
                UPGRADE_WEBSOCKET = 1;

    private static final byte[][] STATUS_CODES = new byte[600][];

    static {
        STATUS_CODES[101] = "101 Switching Protocols".getBytes(StandardCharsets.US_ASCII);
        STATUS_CODES[200] = "200 OK".getBytes(StandardCharsets.US_ASCII);
        STATUS_CODES[400] = "400 Bad Request".getBytes(StandardCharsets.US_ASCII);
        STATUS_CODES[404] = "404 Not Found".getBytes(StandardCharsets.US_ASCII);
        STATUS_CODES[405] = "405 Method Not Allowed".getBytes(StandardCharsets.US_ASCII);
        STATUS_CODES[501] = "501 Not Implemented".getBytes(StandardCharsets.US_ASCII);
    }

    public final StringBuilder s = new StringBuilder();

    private byte httpLvl = 0, upgrade = 0;
    private int state = 0;
    private boolean r = false, n = false, keepAlive = false, upgrading;

    private String s1, s2;
    private WebHandler handler = null;
    private Object attachment = null;

    private String wsVer = null, wsKey = null;

    public ByteBuffer buffer = null;

    @Override
    public void tick() throws Exception {
        ByteBuffer b = buffer != null ? buffer : worker.bl1;
        {
            final int c = transport.read(b);
            if (c == -1) {
                if (state == 1) {
                    transport.close();
                    return;
                }
                throw new IOException("End of Stream");
            }
        }
        b.flip();
        char ch;
        mainLoop:
        while (true)
            switch (state) {
                case 0: // Check for SSL
                    if (!b.hasRemaining()) {
                        b.clear();
                        return;
                    }
                    if (b.get(0) == 0x16) {
                        final SSLEngine engine = server.createEngine();
                        if (engine == null) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        try {
                            final SSLParameters parameters = engine.getSSLParameters();
                            //noinspection Since15
                            parameters.setApplicationProtocols(new String[]{ "http/1.1" }); // Need to implement h2
                            engine.setSSLParameters(parameters);
                        } catch (final Throwable ex) {
                            L.e(ex);
                        }
                        engine.setUseClientMode(false);
                        engine.beginHandshake();
                        transport = new SSLTransport(transport, engine, b);
                        final int c = read(b = worker.bl1 = worker.land.bl1());
                        if (c == -1)
                            throw new IOException("End of Stream");
                        if (c == 0)
                            return;
                    }
                    state = 1;
                case 1: // METHOD
                    while (true) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        ch = (char) (b.get() & 0xFF);
                        if (r) {
                            r = false;
                            if (ch == '\n')
                                continue;
                        }
                        if (ch == ' ') {
                            state = 2;
                            upgrading = false;
                            upgrade = 0;
                            s1 = s.toString();
                            s.setLength(0);
                            break;
                        }
                        if (ch >= 'a' && ch <= 'z')
                            ch -= 32;
                        if (s.length() == 7) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        s.append(ch);
                    }
                case 2: // PATH
                    while (true) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        ch = (char) (b.get() & 0xFF);
                        if (ch == ' ') {
                            state = 3;
                            s2 = s.toString();
                            s.setLength(0);
                            break;
                        }
                        if (ch == '\r' || ch == '\n') {
                            r = ch == '\r';
                            state = 6;
                            httpLvl = 0;
                            keepAlive = false;
                            handler = server.getHandler(s1, s.toString(), "HTTP/0.9", this);
                            handler.setBase(this);
                            s.setLength(0);
                            continue mainLoop;
                        }
                        if (s.length() == 4096) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        s.append(ch);
                    }
                case 3: // PROTOCOL
                    while (true) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        ch = (char) (b.get() & 0xFF);
                        if (ch == '\r' || ch == '\n') {
                            r = ch == '\r';
                            state = 4;
                            final String protocol = s.toString();
                            switch (protocol) {
                                case "HTTP/1.0":
                                    httpLvl = 1;
                                    keepAlive = false;
                                    break;
                                case "HTTP/1.1":
                                    httpLvl = 2;
                                    keepAlive = true;
                                    break;
                                default:
                                    httpLvl = 0;
                                    keepAlive = false;
                                    break;
                            }
                            handler = server.getHandler(s1, s2, protocol, this);
                            handler.setBase(this);
                            s.setLength(0);
                            break;
                        }
                        if (ch >= 'a' && ch <= 'z')
                            ch -= 32;
                        if (s.length() == 8) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        s.append(ch);
                    }
                case 4: // HEADER NAME
                    while (true) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        ch = (char) (b.get() & 0xFF);
                        if (r) {
                            r = false;
                            if (ch == '\n') {
                                n = true;
                                continue;
                            }
                        }
                        if (ch == ':') {
                            s1 = s.toString();
                            s.setLength(0);
                            state = 5;
                            break;
                        }
                        if (ch == '\r' || ch == '\n') {
                            if (s.length() > 0) {
                                handler.header(s.toString());
                                s.setLength(0);
                            }
                            state = upgrading && upgrade == UPGRADE_WEBSOCKET ? 8 : 6;
                            r = ch == '\r';
                            continue mainLoop;
                        }
                        if (ch >= 'A' && ch <= 'Z')
                            ch += 32;
                        if (s.length() == 32) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        s.append(ch);
                    }
                case 5: // HEADER VALUE
                    while (true) {
                        if (!b.hasRemaining()) {
                            b.clear();
                            return;
                        }
                        ch = (char) (b.get() & 0xFF);
                        if ((ch == ' ' || ch == '\t') && s.length() == 0)
                            continue;
                        if (ch == '\r' || ch == '\n') {
                            r = ch == '\r';
                            state = 4;
                            int i = s.length() - 1;
                            for (; i >= 0; i--) {
                                ch = s.charAt(i);
                                if (ch == ' ' || ch == '\t')
                                    continue;
                                break;
                            }
                            s.setLength(++i);
                            switch (s1) {
                                case "sec-websocket-version":
                                    wsVer = s.toString();
                                    break;
                                case "sec-websocket-key":
                                    wsKey = s.toString();
                                    break;
                                case "upgrade":
                                    switch (s.toString().toLowerCase()) {
                                        case "websocket":
                                            upgrade = UPGRADE_WEBSOCKET;
                                            break;
                                        default:
                                            System.out.println("Unknown upgrade: " + s);
                                            break;
                                    }
                                    break;
                                case "connection": {
                                    final String con = s.toString().toLowerCase();
                                    keepAlive = con.equals("keep-alive");
                                    upgrading = con.equals("upgrade");
                                    //System.out.println("Con=" + s);
                                    break;
                                }
                                default:
                                    handler.header(s1, s.toString());
                                    break;
                            }
                            s.setLength(0);
                            continue mainLoop;
                        }
                        if (s.length() == 4096) {
                            b.clear();
                            transport.close();
                            return;
                        }
                        s.append(ch);
                    }
                case 6: { // BODY
                    handler.tick();
                    if (isCurrent())
                        break;
                    if (b.hasRemaining()) {
                        b.compact();
                        if (buffer == null) {
                            buffer = b;
                            worker.bl1 = worker.land.bl1();
                        }
                    } else
                        b.clear();
                    return;
                }
                case 7: // CLOSE
                    if (buffer != null)
                        worker.land.bl1(buffer);
                    else
                        b.clear();
                    transport.close();
                    return;
                case 8: // WEBSOCKET_1
                    transport.interestOps(SelectionKey.OP_WRITE);
                    if (!"13".equals(wsVer)) {
                        state = 7;
                        final ByteBuffer buf = worker.land.bl1();
                        status(buf, 400);
                        header(buf, HEADER_WS_VER);
                        noContent(buf);
                        buf.flip();
                        if (write(buf, true)) {
                            if (buffer != null)
                                worker.land.bl1(buffer);
                            else
                                b.clear();
                            return;
                        } else
                            break;
                    }
                    if (wsKey == null || wsKey.isEmpty()) {
                        state = 9;
                        if (buffer != null)
                            worker.land.bl1(buffer);
                        else
                            b.clear();
                        break;
                    }
                    state = 10;
                    if (b.hasRemaining()) {
                        b.compact();
                        if (buffer == null) {
                            buffer = b;
                            worker.bl1 = worker.land.bl1();
                        }
                    } else
                        b.clear();
                    run(() -> {
                        try {
                            wsKey = Base64.getEncoder().encodeToString(SHA1.get()
                                    .digest((wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                            .getBytes(StandardCharsets.US_ASCII)));
                        } catch (final Throwable ex) {
                            state = 9;
                            L.e(ex);
                        }
                    });
                    return;
                case 9: { // WEBSOCKET_HANDSHAKE_ERROR
                    state = 7;
                    final ByteBuffer buf = worker.land.bl1();
                    status(buf, 400);
                    noContent(buf);
                    buf.flip();
                    if (write(buf, true))
                        return;
                    else
                        break;
                }
                case 10: { // WEBSOCKET_ACCEPT
                    final WSTask t = new WSTask();
                    t.r = r;
                    t.n = n;
                    if (buffer != null) {
                        t.buffer = buffer;
                        buffer.compact();
                    } else if (b.hasRemaining()) {
                        t.buffer = b;
                        b.compact();
                        worker.bl1 = worker.land.bl1();
                    } else
                        b.clear();
                    transport.attach(t.setBase(this));
                    final ByteBuffer buf = worker.land.bl1();
                    status(buf, 101);
                    buf.put(HEADER_UPGRADE_WS).put(LN);
                    buf.put(CON_UPGRADE).put(LN);
                    buf.put(HEADER_WS_ACCEPT).put(wsKey.getBytes(StandardCharsets.US_ASCII)).put(LN);
                    buf.put(LN);

                    buf.put((byte) (0x09 | 0x80));
                    buf.put((byte) 1);
                    buf.put((byte) 3);

                    buf.flip();
                    write(buf, true);
                    t.sendText("Hello, world!");

                    t.sendBinary(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 }), false);
                    return;
                }
                case 11: // KEEP_ALIVE
                    state = 1;
                    transport.interestOps(SelectionKey.OP_READ);
                    break;
                default:
                    throw new RuntimeException("End");
            }
    }

    @Override
    public Object attachment() {
        return attachment;
    }

    @Override
    public void attach(final Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public void status(final ByteBuffer buffer, final int code) {
        if (httpLvl == 0)
            return;
        buffer.put(httpLvl == 2 ? HTTP1_1 : HTTP1_0);
        if (code < 0 || code >= STATUS_CODES.length || STATUS_CODES[code] == null)
            buffer.put((code + " Unknown").getBytes(StandardCharsets.US_ASCII));
        else
            buffer.put(STATUS_CODES[code]);
        buffer.put(LN);
    }

    @Override
    public void header(final ByteBuffer buffer, final String key, final String value) {
        if (httpLvl == 0)
            return;
        buffer.put(key.getBytes(StandardCharsets.US_ASCII)).put(HEADER_SET)
                .put(value.getBytes(StandardCharsets.US_ASCII)).put(LN);
    }

    @Override
    public void header(final ByteBuffer buffer, final String key) {
        if (httpLvl == 0)
            return;
        buffer.put(key.getBytes(StandardCharsets.US_ASCII)).put(LN);
    }

    @Override
    public void header(final ByteBuffer buffer, final byte[] line) {
        if (httpLvl == 0)
            return;
        buffer.put(line).put(LN);
    }

    @Override
    public void noContent(final ByteBuffer buffer) {
        if (httpLvl == 0)
            return;
        buffer.put(NO_DATA).put(LN);
        buffer.put(keepAlive ? KEEP_ALIVE : CLOSED).put(LN);
        buffer.put(LN);
    }

    @Override
    public void content(final ByteBuffer buffer, final int len) {
        if (httpLvl == 0)
            return;
        buffer.put(CONTENT_LENGTH).put(Integer.toString(len).getBytes(StandardCharsets.US_ASCII)).put(LN);
        buffer.put(keepAlive ? KEEP_ALIVE : CLOSED).put(LN);
        buffer.put(LN);
    }

    @Override
    public void end() {
        if (state != 6)
            return;
        if (keepAlive)
            state = 11;
        else
            state = 7;
    }

    @Override
    public void recycle() {
        if (buffer != null)
            worker.land.recycle(buffer);
        super.recycle();
    }
}