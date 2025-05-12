package illa4257.i4Utils.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MultiSocket implements Closeable {
    public final Socket socket;

    private final InputStream inputStream;
    private final OutputStream outputStream, os;

    public MultiSocket(final Socket socket) throws IOException {
        this.socket = socket;
        inputStream = socket.getInputStream();
        os = socket.getOutputStream();
        outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                os.write(MultiSocketFactory.SINGLE_BYTE);
                os.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                os.write(MultiSocketFactory.RAW);
                IO.writeBEInteger(os, len);
                os.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                os.flush();
            }

            @Override
            public void close() throws IOException {
                os.close();
            }
        };
    }

    public void setBufferSizeMode(final boolean auto) throws IOException {
        os.write(MultiSocketFactory.SET_BUFFER_SIZE_MODE);
        os.write(auto ? 1 : 0);
    }

    public void setBufferSize(final int len) throws IOException {
        os.write(MultiSocketFactory.SET_BUFFER_SIZE);
        IO.writeBEInteger(os, len);
    }

    public InputStream getInputStream() { return inputStream; }
    public OutputStream getOutputStream() { return outputStream; }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}