package i4l.common.preprocessors;

public class I4LPError extends I4LPreprocessor {
    public String message;

    public I4LPError(final String message) {
        this.message = message;
    }
}