package i4Framework.base;

public interface IFrameGrabber {
    int fps();
    Image get(final int index);

    IFrameGrabber clone();
}