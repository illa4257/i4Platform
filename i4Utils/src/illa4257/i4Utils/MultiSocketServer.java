package illa4257.i4Utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MultiSocketServer implements Closeable {
    private final MultiSocketFactory m;
    private final Socket s;

    public MultiSocketServer(final MultiSocketFactory multiSocketFactory, final Socket socket) { s = socket; m = multiSocketFactory; }

    public MultiSocketFactory getFactory() { return m; }

    public MultiSocket accept() throws IOException {
        final InputStream is = s.getInputStream();
        synchronized (is) {
            final int a = is.read();
            if (a == -1)
                throw new IOException("End");
            final byte[] code = IO.readByteArray(is, 4);
            final Socket s = m.internalConnect();
            if (s == null)
                throw new IOException("Failed to accept");
            final OutputStream os = s.getOutputStream();
            os.write(MultiSocketFactory.ACCEPT);
            os.write(code);
            os.flush();
            return new MultiSocket(s);
        }
    }

    public void close() throws IOException {
        final OutputStream os = s.getOutputStream();
        synchronized (os) {
            os.write(MultiSocketFactory.CLOSE);
            os.flush();
            s.close();
        }
    }
}