package illa4257.i4Utils.web.base;

import illa4257.i4Utils.io.IO;
import illa4257.i4Utils.web.WebRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static illa4257.i4Utils.web.base.WebFactory.CHARSET;

public abstract class WebOutputStream extends OutputStream {
    public static class Chunked extends WebOutputStream {
        private boolean isClosed = false;
        public final OutputStream outputStream;
        public final ByteArrayOutputStream buffer = IO.BYTE_ARRAY_OUTPUT_STREAM.get();
        public final WebRequest request;

        public Chunked(final OutputStream outputStream, final WebRequest request) {
            this.outputStream = outputStream;
            this.request = request;
        }

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
            outputStream.write((Integer.toString(s, 16) + "\r\n").getBytes(CHARSET));
            buffer.writeTo(outputStream);
            outputStream.write("\r\n".getBytes(CHARSET));
            outputStream.flush();
            buffer.reset();
        }

        @Override
        public void close() throws IOException {
            if (isClosed)
                return;
            flush();
            isClosed = true;
            outputStream.write("0\r\n\r\n".getBytes(CHARSET));
            outputStream.flush();
            request.lastWrittenData = System.currentTimeMillis();
            IO.BYTE_ARRAY_OUTPUT_STREAM.recycle(buffer);
        }
    }
}