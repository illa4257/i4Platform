package illa4257.i4Utils.web;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WebSocket extends WebStream {
    public final boolean isServer;
    public int responseCode;
    public String responseStatus, method, path, protocol;

    public final Map<String, String> headers = new HashMap<String, String>() {
        @Override
        public boolean containsKey(final Object o) {
            if (!(o instanceof String))
                return false;
            return super.containsKey(((String) o).toLowerCase());
        }

        @Override
        public String get(final Object o) {
            if (!(o instanceof String))
                return null;
            return super.get(((String) o).toLowerCase());
        }
    };

    private WebInputStream is;

    public WebSocket(final Socket socket, final boolean isServer) throws IOException {
        super(socket);
        this.isServer = isServer;

        if (isServer) {
            responseCode = 200;
            responseStatus = "OK";
        } else
            method = path = "";

        readData();
    }

    public void readData() throws IOException {
        headers.clear();
        if (isServer) {
            method = readStr(' ', 16);
            path = readStr(' ', 128);
            protocol = readStrLn(16);
        } else {
            protocol = readStr(' ', 16);
            responseCode = Integer.parseInt(readStr(' ', 4));
            responseStatus = readStrLn(32);
        }

        for (int limit = 64; limit > 0; limit--) {
            final String k = readStrLn(':', 64);
            if (k.isEmpty())
                break;
            skipSpaces(4);
            headers.put(k.toLowerCase(), readStrLn(4096));
        }

        if (headers.containsKey("content-length")) {
            is = new WebInputStream.LongPolling(inputStream, Integer.parseInt(headers.get("content-length")));
            return;
        }
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
            is = new WebInputStream.Chunked(inputStream);
            return;
        }
        is = new WebInputStream.LongPolling(inputStream, 0);
    }

    public WebSocket(final WebStream stream, final boolean isServer) throws IOException { this(stream.socket, isServer); }

    public WebInputStream getInputStream() { return is; }
}