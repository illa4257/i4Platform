package i4l.common;

import java.util.List;

public class I4LCallAnnotation extends I4LOperation {
    public Object target;
    public List<Object> args = null;

    public I4LCallAnnotation(final String name) { this.target = name; }
}