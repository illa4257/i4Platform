package illa4257.i4Utils.web;

import java.io.IOException;
import java.io.InputStream;

public abstract class WSInputStream extends InputStream {
    public boolean stream = true;
    public int closeCode = -1;

    public abstract int nextPacket() throws IOException;
}