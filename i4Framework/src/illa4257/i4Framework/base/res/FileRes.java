package illa4257.i4Framework.base.res;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileRes implements Res {
    public final File file;
    private volatile boolean isNotAssignedMimeType = true;
    private volatile String mimeType;

    public FileRes(final File file) {
        this.file = file;
    }

    @Override
    public String getMimeType() throws IOException {
        if (isNotAssignedMimeType)
            synchronized (this) {
                if (isNotAssignedMimeType) {
                    mimeType = Files.probeContentType(file.toPath());
                    isNotAssignedMimeType = false;
                }
            }
        return mimeType;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(file.toPath());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return Files.newOutputStream(file.toPath());
    }

    @Override
    public boolean delete() {
        return file.delete();
    }
}