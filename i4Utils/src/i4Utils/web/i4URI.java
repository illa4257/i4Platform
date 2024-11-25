package i4Utils.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class i4URI {
    public final int port;

    public final String
            scheme,
            domain,
            path;

    public final char[][] userInfo;

    private static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    private static char[][] parseUserInfo(final String userInfo) {
        if (isEmpty(userInfo))
            return new char[0][];
        final String[] l = userInfo.split(":");
        final ArrayList<char[]> ll = new ArrayList<>();
        for (final String d : l)
            if (!d.isEmpty())
                ll.add(d.toCharArray());
        return ll.toArray(new char[1][]);
    }

    public i4URI(
            final String scheme,
            final char[][] userInfo,
            final String domain,
            final int port,
            final String path
    ) {
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");
        if (isEmpty(domain))
            throw new IllegalArgumentException("Domain cannot be null or empty");
        this.scheme = scheme.toLowerCase();
        this.userInfo = userInfo == null ? new char[0][] : userInfo;
        this.domain = domain.toLowerCase();
        this.port = port;
        this.path = isEmpty(path) ? null : path;
    }

    public i4URI(
            final String scheme,
            final String domain,
            final int port,
            final String path
    ) {
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");
        if (isEmpty(domain))
            throw new IllegalArgumentException("Domain cannot be null or empty");
        this.scheme = scheme.toLowerCase();
        this.userInfo = new char[0][];
        this.domain = domain.toLowerCase();
        this.port = port;
        this.path = isEmpty(path) ? null : path;
    }

    public i4URI(
            final String scheme,
            final String domain,
            final String path
    ) {
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");
        if (isEmpty(domain))
            throw new IllegalArgumentException("Domain cannot be null or empty");
        this.scheme = scheme.toLowerCase();
        this.userInfo = new char[0][];
        this.domain = domain.toLowerCase();
        this.port = -1;
        this.path = isEmpty(path) ? null : path;
    }

    public i4URI(final URI uri) {
        this.scheme = uri.getScheme();
        this.userInfo = parseUserInfo(uri.getUserInfo());
        this.domain = uri.getHost();
        this.port = uri.getPort();
        this.path = uri.getPath();
    }

    public i4URI(String uri, final String defaultScheme) {
        if (isEmpty(uri))
            throw new IllegalArgumentException("URI cannot be null or empty");
        int colon = uri.indexOf(':'), splash = uri.indexOf('/'), atSymbol, i;

        // Scheme
        if (colon > -1 && (colon == splash - 1 || splash == -1)) {
            scheme = colon == 0 ? defaultScheme : uri.substring(0, colon);
            uri = uri.substring(colon + 1);
        } else
            scheme = defaultScheme;
        if (isEmpty(scheme))
            throw new IllegalArgumentException("Scheme cannot be null or empty");

        final int ul = uri.length();
        for (i = 0; i < ul; i++)
            if (uri.charAt(i) != '/')
                break;
        if (i > 0)
            uri = uri.substring(i);

        // User Info
        atSymbol = uri.indexOf('@');
        splash = uri.indexOf('/');
        if (atSymbol > -1 && (atSymbol < splash || splash == -1)) {
            userInfo = parseUserInfo(uri.substring(0, atSymbol));
            uri = uri.substring(atSymbol + 1);
            splash = uri.indexOf('/');
        } else
            userInfo = new char[0][];
        colon = uri.indexOf(':');

        // Domain
        if (colon == -1 && splash == -1) {
            domain = uri;
            port = -1;
            path = null;
            return;
        }
        if (colon > -1 && (colon < splash || splash == -1)) {
            domain = uri.substring(0, colon);
            colon++;
            if (splash == -1) {
                port = Integer.parseInt(uri.substring(colon));
                path = null;
                return;
            }
            port = Integer.parseInt(uri.substring(colon, splash));
        } else {
            domain = uri.substring(0, splash);
            port = -1;
        }
        path = uri.substring(splash);
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
                        Objects.equals(path, i4URI.path);
    }

    @Override public int hashCode() { return Objects.hash(scheme, Arrays.deepHashCode(userInfo), domain, port, path); }

    @Override public String toString() {
        return scheme + "://" + domain + (port > -1 ? ":" + port : "") + (path != null ? path : "");
    }
}