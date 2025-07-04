package illa4257.i4Utils;

import java.io.Serializable;

public class SemVer implements Comparable<SemVer>, Serializable {
    private static final long serialVersionUID = 1L;

    public enum ReleaseType {
        RELEASE,
        RELEASE_CANDIDATE,
        PRE_RELEASE,
        BETA,
        ALPHA;

        public static ReleaseType of(final String tag) {
            if (tag == null)
                return RELEASE;
            switch (tag.toLowerCase()) {
                case "rc":
                    return ReleaseType.RELEASE_CANDIDATE;
                case "pre-release":
                    return ReleaseType.PRE_RELEASE;
                case "b":
                case "beta":
                    return ReleaseType.BETA;
                case "a":
                case "alpha":
                    return ReleaseType.ALPHA;
                default:
                    return ReleaseType.RELEASE;
            }
        }
    }

    public final int major, minor, patch, build, revision;
    public final String tag;
    public final ReleaseType releaseType;

    public SemVer(final int major, final int minor, final int patch, final int build, final int revision, final String tag) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.build = build;
        this.revision = revision;
        this.tag = tag;
        this.releaseType = ReleaseType.of(tag);
    }

    public SemVer(String version) {
        version = version.trim();
        final int l = version.length();
        int i = 0;
        char ch = ' ';
        StringBuilder b = new StringBuilder();
        for (; i < l && Str.CHARS_NUMS.contains(ch = version.charAt(i)); i++)
            b.append(ch);
        major = i > 0 ? Integer.parseInt(b.toString()) : 0;
        if (ch == '.') {
            while (i < l && !Str.CHARS_NUMS.contains(version.charAt(i)))
                i++;
            b = new StringBuilder();
            for (; i < l && Str.CHARS_NUMS.contains(ch = version.charAt(i)); i++)
                b.append(ch);
            minor = b.length() > 0 ? Integer.parseInt(b.toString()) : 0;
            if (ch == '.') {
                while (i < l && !Str.CHARS_NUMS.contains(version.charAt(i)))
                    i++;
                b = new StringBuilder();
                for (; i < l && Str.CHARS_NUMS.contains(ch = version.charAt(i)); i++)
                    b.append(ch);
                patch = b.length() > 0 ? Integer.parseInt(b.toString()) : 0;
            } else
                patch = 0;
        } else
            minor = patch = 0;
        b = new StringBuilder();
        if (ch == '-' || ch == ' ') {
            i++;
            while (i < l && !Str.CHARS_NUMS.contains(ch = version.charAt(i)) && ch != '+') {
                b.append(ch);
                i++;
            }
            String s = b.toString().trim();
            if (s.endsWith("."))
                s = s.substring(0, s.length() - 1);
            tag = !s.isEmpty() && !s.equalsIgnoreCase("build") ? s : null;
            releaseType = ReleaseType.of(s);
        } else {
            tag = null;
            releaseType = ReleaseType.RELEASE;
        }
        if (ch == '.' || ch == '+' || Str.CHARS_NUMS.contains(ch)) {
            while (i < l && !Str.CHARS_NUMS.contains(version.charAt(i)))
                i++;
            b = new StringBuilder();
            for (; i < l && Str.CHARS_NUMS.contains(ch = version.charAt(i)); i++)
                b.append(ch);
            build = b.length() > 0 ? Integer.parseInt(b.toString()) : 0;
            if (ch == '.') {
                while (i < l && !Str.CHARS_NUMS.contains(version.charAt(i)))
                    i++;
                b = new StringBuilder();
                for (; i < l && Str.CHARS_NUMS.contains(ch = version.charAt(i)); i++)
                    b.append(ch);
                revision = b.length() > 0 ? Integer.parseInt(b.toString()) : 0;
            } else
                revision = 0;
        } else
            build = revision = 0;
    }

    @Override
    public int compareTo(final SemVer o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        if (patch != o.patch) return Integer.compare(patch, o.patch);
        if (build != o.build) return Integer.compare(build, o.build);
        if (revision != o.revision) return Integer.compare(revision, o.revision);
        return releaseType.compareTo(o.releaseType);
    }

    public String format() {
        return major + "." + minor + "." + patch + (tag != null ? "-" + tag : "") + "+" + build;
    }

    @Override
    public String toString() {
        return "Version{major=" + major + ", minor=" + minor + ", patch=" + patch + ", build=" + build + ", revision=" + revision + ", tag=" + tag + ", releaseType=" + releaseType + '}';
    }
}