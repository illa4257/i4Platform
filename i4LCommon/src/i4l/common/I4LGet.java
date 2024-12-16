package i4l.common;

public class I4LGet extends I4LOperation {
    public String path;

    public I4LGet(final String path) { this.path = path; }

    @Override public String toString() { return "GET " + path; }
}