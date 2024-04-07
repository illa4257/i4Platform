package i4l.common;

public class I4LGet {
    public String path = null;

    public I4LGet(final String path) { this.path = path; }

    @Override
    public String toString() {
        return "GET " + path;
    }
}