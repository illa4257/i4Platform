package i4Utils.web;

import i4Utils.Str;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class WebSocket extends WebStream {
    public final int responseCode;
    public final String responseStatus;

    public final Map<String, String> headers;

    private final WebInputStream is;

    public WebSocket(final Socket socket) throws IOException {
        super(socket);

        readStr(' ', 16); // Skip protocol/version
        responseCode = Integer.parseInt(readStr(' ', 4));
        responseStatus = readStrLn(32);

        headers = new HashMap<String, String>() {
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

        for (int limit = 64; limit > 0; limit--) {
            final String k = readStrLn(':', 64);
            if (k.isEmpty())
                break;
            skipSpaces(4);
            headers.put(k.toLowerCase(), readStrLn(4096));
        }

        System.out.println(headers);

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

    public WebSocket(final WebStream stream) throws IOException { this(stream.socket); }

    public WebInputStream getInputStream() { return is; }
}