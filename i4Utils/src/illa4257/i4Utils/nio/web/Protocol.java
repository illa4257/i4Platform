package illa4257.i4Utils.nio.web;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Protocol {
    Object attachment();
    void attach(final Object attachment);

    void status(final ByteBuffer buffer, final int code);
    void header(final ByteBuffer buffer, final String key, final String value);
    void header(final ByteBuffer buffer, final String key);
    void header(final ByteBuffer buffer, final byte[] line);

    void noContent(final ByteBuffer buffer);
    void content(final ByteBuffer buffer, final int len);

    boolean isBodyOpen();
    boolean isLastPart() throws IOException;
    ByteBuffer getBody() throws IOException;
    void updateBody() throws IOException;

    void end() throws IOException;
}