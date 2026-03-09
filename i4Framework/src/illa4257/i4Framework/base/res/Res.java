package illa4257.i4Framework.base.res;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Res {
    String getMimeType() throws IOException;
    InputStream openInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    boolean delete();
}