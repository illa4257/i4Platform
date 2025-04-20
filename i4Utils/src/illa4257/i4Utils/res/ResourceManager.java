package illa4257.i4Utils.res;

import illa4257.i4Utils.web.i4URI;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager implements ResourceProvider {
    public final ConcurrentHashMap<String, ResourceProvider> fileSystems = new ConcurrentHashMap<>();

    public ResourceManager add(final String scheme, final ResourceProvider provider) {
        fileSystems.put(scheme, provider);
        return this;
    }

    public ResourceManager add(final ResourceProvider provider) {
        provider.addTo(this);
        return this;
    }

    @Override
    public InputStream openResource(final i4URI uri) {
        final ResourceProvider fs = fileSystems.get(uri.scheme);
        return fs != null ? fs.openResource(uri) : null;
    }
}