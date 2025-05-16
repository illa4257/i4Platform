package illa4257.i4Utils.web;

import java.io.IOException;
import java.io.OutputStream;

public abstract class WSOutputStream extends OutputStream {
    public abstract void write(final String str) throws IOException;


    public abstract void close(final int code, final String reason) throws IOException;

    @Override public void close() throws IOException { close(1000, ""); }
}