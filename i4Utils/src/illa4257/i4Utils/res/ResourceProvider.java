package illa4257.i4Utils.res;

import illa4257.i4Utils.web.i4URI;

import java.io.InputStream;

public interface ResourceProvider {
    InputStream openResource(final i4URI uri);
    default InputStream openResource(final String path) { return openResource(new i4URI(path)); }
    default void addTo(final ResourceManager mgr) {}
}