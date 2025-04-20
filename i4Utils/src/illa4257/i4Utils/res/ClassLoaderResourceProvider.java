package illa4257.i4Utils.res;

import illa4257.i4Utils.web.i4URI;

import java.io.InputStream;

public class ClassLoaderResourceProvider implements ResourceProvider {
    public final ClassLoader classLoader;

    public ClassLoaderResourceProvider(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public InputStream openResource(final i4URI uri) {
        return classLoader.getResourceAsStream(uri.fullPath);
    }
}
