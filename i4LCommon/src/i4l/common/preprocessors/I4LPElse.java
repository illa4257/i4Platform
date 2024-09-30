package i4l.common.preprocessors;

public class I4LPElse extends I4LPreprocessor {
    public I4LPreprocessor another;

    public I4LPElse() {
        another = null;
    }

    public I4LPElse(final I4LPreprocessor another) {
        this.another = another;
    }
}