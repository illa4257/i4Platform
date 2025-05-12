package illa4257.i4Utils.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class WebOutputStream extends OutputStream {
    public static class Chunked extends WebOutputStream {
        private boolean isClosed = false;
        public final OutputStream outputStream;
        public final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public Chunked(final OutputStream outputStream) { this.outputStream = outputStream; }

        @Override
        public void write(final int b) throws IOException {
            if (isClosed)
                return;
            buffer.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (isClosed)
                return;
            buffer.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            final int s = buffer.size();
            if (isClosed || s == 0)
                return;
            outputStream.write((Integer.toString(s, 16) + "\r\n").getBytes(WebClientFactory.CHARSET));
            buffer.writeTo(outputStream);
            outputStream.write("\r\n".getBytes(WebClientFactory.CHARSET));
            outputStream.flush();
            buffer.reset();
        }

        @Override
        public void close() throws IOException {
            if (isClosed)
                return;
            flush();
            isClosed = true;
            outputStream.write("0\r\n\r\n".getBytes(WebClientFactory.CHARSET));
            outputStream.flush();
        }
    }
}