package illa4257.i4Framework.base.graphics;

import illa4257.i4Utils.media.Image;

public interface IFrameGrabber {
    int fps();
    Image get(final int index);

    IFrameGrabber clone();
}