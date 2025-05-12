package illa4257.i4Utils.integration;

import java.io.IOException;
import java.io.OutputStream;

public class CheerpJControllerReader extends OutputStream {
    static { System.loadLibrary("i4Utils.cheerpj"); }

    public final Object controller;

    public CheerpJControllerReader(final Object controller) { this.controller = controller; }

    private native void writeByte(final int b) throws IOException;

    @Override public void write(final int b) throws IOException { writeByte(b); }
    @Override public native void write(final byte[] b, final int off, final int len) throws IOException;

    @Override
    public native void close() throws IOException;
}