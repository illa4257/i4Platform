package illa4257.i4test;

import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;

public class NioHttpSession extends NioRunner.Session {
    public static final byte[] LN = "\r\n".getBytes(StandardCharsets.US_ASCII),
                CONTENT_LENGTH = "Content-Length: ".getBytes(StandardCharsets.US_ASCII),
                CONNECTION_CLOSED = "Connection: closed\r\n\r\n".getBytes(StandardCharsets.US_ASCII),
                CONNECTION_KEEPALIVE = "Connection: keep-alive\r\n\r\n".getBytes(StandardCharsets.US_ASCII),
                CONNECTION_UPGRADE = "Connection: Upgrade\r\n\r\n".getBytes(StandardCharsets.US_ASCII)
    ;

    public static final byte[][] STATUS = new byte[600][];

    public static void register(final int code, final String status) {
        STATUS[code] = (" " + code + " " + status).getBytes(StandardCharsets.US_ASCII);
    }

    static {
        register(101, "Switching Protocols");
        register(200, "OK");
        register(400, "Bad Request");
        register(404, "Not Found");
        register(500, "Internal Server Error");
    }

    public byte readStatus = 0;
    public final StringBuilder b = new StringBuilder();
    public boolean rc = false, keepAlive = false;
    public String method = null, path = null, protocol = null;
    private String headerKey = null;

    public byte[] responseLine = null, response = null;

    public NioHttpSession() { responseLine = STATUS[200]; }

    public void writeStatus(final ByteBuffer out) {
        boolean old = false; /// Use to prevent unsupported features like chunked streaming.
        switch (protocol) {
            case "HTTP/0.9":
                if (response != null)
                    out.put(response);
                break;
            case "HTTP/1.0":
                old = true;
            case "HTTP/1.1":
                out.put(protocol.getBytes(StandardCharsets.US_ASCII));
                out.put(responseLine);
                out.put(LN);
                break;
            default:
                throw new RuntimeException("Unknown protocol: " + protocol);
        }
    }

    public void writeHeader(final ByteBuffer out, final String key, final String value) {
        if (protocol.equals("HTTP/0.9"))
            return;
        out.put((key + ": " + value).getBytes(StandardCharsets.US_ASCII));
        out.put(LN);
    }

    public void writeEndHeaders(final ByteBuffer out) {
        if (response != null) {
            out.put(CONTENT_LENGTH);
            out.put(Integer.toString(response.length).getBytes(StandardCharsets.US_ASCII));
            out.put(LN);
        }
        out.put(keepAlive ? CONNECTION_KEEPALIVE : CONNECTION_CLOSED);
        if (response != null)
            out.put(response);
    }

    public void closeRequest(final ByteBuffer out) throws Exception {
        if (keepAlive) {
            reset();
            keepAlive = true;
            if (netOut != null || (out.hasRemaining() && write(out)))
                return;
            key.interestOps(SelectionKey.OP_READ);
        } else {
            if (netOut != null || (out.hasRemaining() && write(out))) {
                close = true;
                return;
            }
            close();
        }
    }

    public void onStart() throws Exception {}

    public void header(String name, final StringBuilder value) throws Exception {
        name = name.trim().toLowerCase();
        if ("connection".equals(name))
            for (final String v : value.toString().split(";"))
                if (v.trim().equalsIgnoreCase("keep-alive")) {
                    keepAlive = true;
                    return;
                } else
                    i4Logger.INSTANCE.w("Unknown connection feature:", value);
        else
            i4Logger.INSTANCE.i("HEADER", name, "=", value);
    }

    public void onContentStart() throws Exception {}
    public void onTick() throws Exception {}

    @Override
    public void process() throws Exception {
        if (old == -1 && read() == -1)
            throw new IOException("Closed");
        final ByteBuffer ai = getAppIn();
        ai.flip();
        if (old != -1) {
            //noinspection MagicConstant
            key.interestOps(old);
            old = -1;
        } else if (!ai.hasRemaining()) {
            compactAppIn();
            return;
        }

        if (readStatus < 5) {
            int l;
            char ch;
            if (readStatus == 0) {
                for (l = b.length(); l < 12; l++) {
                    if (!ai.hasRemaining()) {
                        compactAppIn();
                        return;
                    }
                    ch = (char) (ai.get() & 0xFF);
                    if (ch == ' ') {
                        readStatus++;
                        method = b.toString();
                        b.setLength(0);
                        break;
                    }
                    b.append(ch);
                }
                if (l == 12)
                    throw new RuntimeException("Method is too long!");
            }

            if (readStatus == 1) {
                for (l = b.length(); l < 2048; l++) {
                    if (!ai.hasRemaining()) {
                        compactAppIn();
                        return;
                    }
                    ch = (char) (ai.get() & 0xFF);
                    if (ch == '\n' || rc) {
                        rc = false;
                        readStatus = 6;
                        path = b.toString();
                        protocol = "HTTP/0.9";
                        b.setLength(0);
                        onStart();
                        if (ch != '\n' && ch != '\r')
                            b.append(ch);
                        onContentStart();
                        break;
                    }
                    if (ch == ' ') {
                        readStatus++;
                        path = b.toString();
                        b.setLength(0);
                        break;
                    }
                    if (ch == '\r') {
                        rc = true;
                        continue;
                    }
                    b.append(ch);
                }
                if (l == 2048)
                    throw new RuntimeException("Method is too long!");
            }

            if (readStatus == 2) {
                for (l = b.length(); l < 10; l++) {
                    if (!ai.hasRemaining()) {
                        compactAppIn();
                        return;
                    }
                    ch = (char) (ai.get() & 0xFF);
                    if (ch == '\n' || rc) {
                        rc = false;
                        readStatus++;
                        protocol = b.toString().toUpperCase();
                        b.setLength(0);
                        if (ch == '\r')
                            readStatus = 5;
                        else if (ch != '\n')
                            b.append(ch);
                        onStart();
                        break;
                    }
                    if (ch == '\r') {
                        rc = true;
                        continue;
                    }
                    b.append(ch);
                }
                if (l == 10)
                    throw new RuntimeException("Method is too long!");
            }

            while (readStatus < 5) {
                if (readStatus < 3)
                    break;
                if (readStatus == 3) { // header name
                    for (l = b.length(); l < 256; l++) {
                        if (!ai.hasRemaining()) {
                            compactAppIn();
                            return;
                        }
                        ch = (char) (ai.get() & 0xFF);
                        if (ch == '\n' || rc) {
                            rc = false;
                            if (b.length() > 0)
                                i4Logger.INSTANCE.e(b);
                            readStatus = 5;
                            b.setLength(0);
                            if (ch != '\n' && ch != '\r')
                                b.append(ch);
                            onContentStart();
                            break;
                        }
                        if (ch == '\r') {
                            rc = true;
                            continue;
                        }
                        if (ch == ':') {
                            headerKey = b.toString();
                            b.setLength(0);
                            readStatus++;
                            break;
                        }
                        b.append(ch);
                    }
                    if (l == 256)
                        throw new RuntimeException("The header name is too long!");
                }

                if (readStatus == 4) { // header value
                    for (l = b.length(); l < 4096; l++) {
                        if (!ai.hasRemaining()) {
                            compactAppIn();
                            return;
                        }
                        ch = (char) (ai.get() & 0xFF);
                        if (ch == '\n' || rc) {
                            rc = false;
                            readStatus--;
                            header(headerKey, b);
                            headerKey = null;
                            readStatus = 3;
                            b.setLength(0);
                            if (ch != '\r' && ch != '\n')
                                b.append(ch);
                            else if (ch == '\r') {
                                readStatus = 5;
                                i4Logger.INSTANCE.i(method, path, protocol);
                                onContentStart();
                            }
                            break;
                        }
                        if (ch == '\r') {
                            rc = true;
                            continue;
                        }
                        b.append(ch);
                    }
                    if (l == 4096)
                        throw new RuntimeException("The header value is too long!");
                }
            }

            i4Logger.INSTANCE.i(method, path, protocol);
        }

        onTick();
        Thread.sleep(250);

        compactAppIn();
    }

    @Override
    public void reset() {
        super.reset();
        keepAlive = false;
        readStatus = 0;
        responseLine = STATUS[200];
        response = null;
    }
}