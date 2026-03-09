package illa4257.i4Framework.android;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import illa4257.i4Framework.base.res.Res;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UriRes implements Res {
    public final Uri uri;
    public final ContentResolver resolver;

    public UriRes(final Context context, final Uri uri)  {
        this.uri = uri;
        this.resolver = context.getContentResolver();
    }

    @Override
    public String getMimeType() {
        return resolver.getType(uri);
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return resolver.openInputStream(uri);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return resolver.openOutputStream(uri);
    }

    @Override
    public boolean delete() {
        try {
            return DocumentsContract.deleteDocument(resolver, uri);
        } catch (final FileNotFoundException ignored) {
            return false;
        }
    }
}
