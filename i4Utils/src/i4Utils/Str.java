package i4Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Str {
    public static final String
            CHARS_NUMS = "0123456789",
            CHARS_EN_LOW = "abcdefghijklmnopqrstuvwxyz",
            CHARS_EN_UP = CHARS_EN_LOW.toUpperCase(),
            UTF_8 = StandardCharsets.UTF_8.toString(),
            URI_CHARS = CHARS_EN_LOW + CHARS_EN_UP + CHARS_NUMS + ":/?#[]@!$&'()*+,;=-._~%"
    ;

    public static String encodeURI(final String uri, final boolean isQuery) throws UnsupportedEncodingException {
        final String space = isQuery ? "+" : "%20";
        final StringBuilder builder = new StringBuilder();

        for (final char ch : uri.toCharArray())
            if (URI_CHARS.indexOf(ch) == -1)
                builder.append(URLEncoder.encode(String.valueOf(ch), UTF_8));
            else if (ch == ' ')
                builder.append(space);
            else
                builder.append(ch);

        return builder.toString();
    }
}