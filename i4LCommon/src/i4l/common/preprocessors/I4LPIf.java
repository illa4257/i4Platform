package i4l.common.preprocessors;

public class I4LPIf extends I4LPreprocessor {
    public I4LPreprocessor condition;

    public I4LPIf(final I4LPreprocessor condition) {
        this.condition = condition;
    }
}