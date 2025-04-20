package illa4257.i4Utils.res;

import illa4257.i4Utils.web.i4URI;

import java.io.InputStream;

public class SubResourceProvider implements ResourceProvider {
    public final ResourceProvider subResourceProvider;

    public SubResourceProvider(final ResourceProvider subResourceProvider) {
        this.subResourceProvider = subResourceProvider;
    }

    @Override
    public InputStream openResource(final i4URI uri) {
        return subResourceProvider.openResource(uri.fullPath);
    }
}