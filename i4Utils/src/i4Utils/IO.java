package i4Utils;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class IO {
    public static byte[] toBytes(final Charset charset, final char[] charArray) {
        return (charset != null ? charset : Charset.defaultCharset()).encode(CharBuffer.wrap(charArray)).array();
    }
}