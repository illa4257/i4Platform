package illa4257.i4Utils.web;

import java.net.URI;
import java.util.*;

public class i4URI {
    public final int port;

    public final String
            scheme,
            domain,
            fullPath,
            path;

    public final char[][] userInfo;

    /// Any changes will not be represented in the field called `path`.
    public final Map<String, String> queries;

    private static boolean isEmpty(final String str) { return str == null || str.isEmpty(); }

    public static char[][] parseUserInfo(final String userInfo) {
        if (isEmpty(userInfo))
            return new char[0][];
        final String[] l = userInfo.split(":");
        final ArrayList<char[]> ll = new ArrayList<>();
        for (final String d : l)
            if (!d.isEmpty())
                ll.add(d.toCharArray());
        return ll.toArray(new char[1][]);
    }

    public static void parseQueries(final Map<String, String> map, final String queries) {
        if (map == null)
            throw new IllegalArgumentException("Map cannot be null");
        if (queries == null || queries.isEmpty())
            return;
        for (final String query : queries.split("&")) {
            if (query.isEmpty())
                continue;
            final int eq = query.indexOf('=');
            if (eq == -1) {
                map.put(query, null);
                continue;
            }
            map.put(query.substring(0, eq), query.substring(eq + 1));
        }
    }

    public i4URI(
            final String scheme,
            final char[][] userInfo,
            final String domain,
            final int port,
            final String fullPath
    ) {
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");
        if (isEmpty(domain))
            throw new IllegalArgumentException("Domain cannot be null or empty");
        this.scheme = scheme.toLowerCase();
        this.userInfo = userInfo == null ? new char[0][] : userInfo;
        this.domain = domain.toLowerCase();
        this.port = port;
        this.fullPath = isEmpty(fullPath) ? null : fullPath;
        if (this.fullPath == null) {
            this.queries = null;
            this.path = null;
            return;
        }
        this.queries = new HashMap<>();
        final int q = this.fullPath.indexOf('?');
        if (q == -1) {
            this.path = this.fullPath;
            return;
        }
        this.path = this.fullPath.substring(0, q);
        parseQueries(this.queries, this.fullPath.substring(q + 1));
    }

    public i4URI(
            final String scheme,
            final String domain,
            final int port,
            final String fullPath
    ) {
        this(scheme, null, domain, port, fullPath);
    }

    public i4URI(
            final String scheme,
            final String domain,
            final String path
    ) {
        this(scheme, null, domain, -1, path);
    }

    public i4URI(final URI uri) {
        scheme = uri.getScheme();
        userInfo = parseUserInfo(uri.getUserInfo());
        domain = uri.getHost();
        port = uri.getPort();
        path = uri.getPath();
        queries = new HashMap<>();
        final String q = uri.getQuery();
        if (q == null) {
            fullPath = path;
            return;
        }
        fullPath = path + '?' + q;
        parseQueries(queries, q);
    }

    public static i4URI resolve(final String uri, final i4URI base) {
        return new i4URI(uri, base.scheme, new char[0][], base.domain, base.port, base.path);
    }

    public i4URI(String uri, final String defaultScheme, final char[][] defaultUserInfo, final String defaultDomain, final int defaultPort, final String defaultPath) {
        if (isEmpty(uri))
            throw new IllegalArgumentException("URI cannot be null or empty");
        int colon = uri.indexOf(':'), splash = uri.indexOf('/'), atSymbol, query, min, i;

        // Scheme
        if (colon > -1 && (colon < splash || splash == -1)) {
            scheme = colon == 0 ? defaultScheme : uri.substring(0, colon);
            uri = uri.substring(colon + 1);
        } else
            scheme = defaultScheme;
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");

        final int ul = Math.min(uri.length(), 2);
        for (i = 0; i < ul; i++)
            if (uri.charAt(i) != '/')
                break;
        if (i > 1) {
            uri = uri.substring(i);

            // User Info
            atSymbol = uri.indexOf('@');
            splash = uri.indexOf('/');
            query = uri.indexOf('?');
            if (splash != -1 && query != -1)
                min = Math.min(splash, query);
            else if (query == -1)
                min = splash;
            else
                min = query;
            if (atSymbol > -1 && (atSymbol < min || min == -1)) {
                userInfo = parseUserInfo(uri.substring(0, atSymbol));
                uri = uri.substring(atSymbol + 1);
                if (splash != -1)
                    splash = uri.indexOf('/');
                if (query != -1)
                    query = uri.indexOf('?');
                if (splash != -1 && query != -1)
                    min = Math.min(splash, query);
                else if (query == -1)
                    min = splash;
                else
                    min = query;
            } else
                userInfo = defaultUserInfo;
            colon = uri.indexOf(':');

            // Domain
            if (colon == -1 && min == -1) {
                domain = uri;
                port = defaultPort;
                fullPath = path = null;
                queries = null;
                return;
            }
            if (colon > -1 && (colon < min || min == -1)) {
                domain = uri.substring(0, colon);
                colon++;
                if (min == -1) {
                    port = Integer.parseInt(uri.substring(colon));
                    fullPath = path = null;
                    queries = null;
                    return;
                }
                port = Integer.parseInt(uri.substring(colon, min));
            } else {
                domain = uri.substring(0, min);
                port = -1;
            }
        } else {
            splash = min = 0;
            port = defaultPort;
            domain = defaultDomain;
            userInfo = defaultUserInfo;
        }

        queries = new HashMap<>();
        String tmp1 = splash == min ? uri.substring(splash) : '/' + uri.substring(min), tmp2;
        query = tmp1.indexOf('?');
        if (query == -1) {
            if (tmp1.equals("."))
                tmp1 = defaultPath;
            path = fullPath = tmp1;
            return;
        }
        parseQueries(queries, tmp1.substring(query + 1));

        tmp2 = tmp1.substring(0, query);
        if (tmp2.equals(".")) {
            path = defaultPath;
            fullPath = defaultPath + tmp1.substring(query);
            return;
        }
        path = tmp2;
        fullPath = tmp1;
    }

    public i4URI(String uri, final String defaultScheme) {
        this(uri, defaultScheme, new char[0][], "", -1, "/");
    }

    public i4URI(final String uri) { this(uri, null); }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final i4URI i4URI = (i4URI) o;
        return port == i4URI.port &&
                        Objects.equals(scheme, i4URI.scheme) &&
                        Arrays.deepEquals(userInfo, i4URI.userInfo) &&
                        Objects.equals(domain, i4URI.domain) &&
                        Objects.equals(fullPath, i4URI.fullPath);
    }

    @Override public int hashCode() { return Objects.hash(scheme, Arrays.deepHashCode(userInfo), domain, port, fullPath); }

    @Override public String toString() {
        return scheme + "://" + domain + (port > -1 ? ":" + port : "") + (fullPath != null ? fullPath : "");
    }
}