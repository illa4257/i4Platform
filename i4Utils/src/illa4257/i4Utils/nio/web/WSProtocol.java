package illa4257.i4Utils.nio.web;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WSProtocol {
    void sendText(final String text) throws IOException;
    void sendBinary(final ByteBuffer binary, final boolean recycle) throws IOException;

    /**
     * @return 1 is a text, 2 is a binary.
     */
    byte getFrameType();

    long remainingFrameBytes();

    void skipRest();
}