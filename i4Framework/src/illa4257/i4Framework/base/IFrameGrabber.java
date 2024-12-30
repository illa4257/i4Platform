package illa4257.i4Framework.base;

public interface IFrameGrabber {
    int fps();
    Image get(final int index);

    IFrameGrabber clone();
}