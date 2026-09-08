package illa4257.i4Framework.base.res;

import illa4257.i4Utils.annotations.Experimental;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface Res {
    String getMimeType() throws IOException;
    InputStream openInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    boolean delete();

    @Experimental String getName();
    @Experimental Path toPath();
}