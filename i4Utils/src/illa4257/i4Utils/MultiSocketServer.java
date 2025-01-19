package illa4257.i4Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MultiSocketServer {
    private final MultiSocketFactory m;
    private final Socket s;

    public MultiSocketServer(final MultiSocketFactory multiSocketFactory, final Socket socket) { s = socket; m = multiSocketFactory; }

    public MultiSocketFactory getFactory() { return m; }

    public MultiSocket accept() throws IOException {
        final InputStream is = s.getInputStream();
        synchronized (s) {
            System.out.println("Reading");
            final int a = is.read();
            System.out.println("MSG: " + a);
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
}