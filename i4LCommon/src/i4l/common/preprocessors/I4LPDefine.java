package i4l.common.preprocessors;

public class I4LPDefine extends I4LPreprocessor {
    public String name;
    public Object value = null;

    public I4LPDefine(final String name) {
        this.name = name;
    }
}