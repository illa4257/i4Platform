package illa4257.i4Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

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

    public static StringBuilder repeat(final StringBuilder builder, final String str, int n) {
        for (; n > 0; n--)
            builder.append(str);
        return builder;
    }

    public static String repeat(final String str, final int n) { return repeat(new StringBuilder(), str, n).toString(); }

    public static <T> StringBuilder join(final StringBuilder builder, final String delimiter, final List<T> items, Function<T, String> func) {
        if (items == null || items.isEmpty())
            return builder;
        builder.append(func.apply(items.get(0)));
        final int l = items.size();
        for (int i = 1; i < l; i++)
            builder.append(delimiter).append(func.apply(items.get(i)));
        return builder;
    }

    public static <T> String join(final String delimiter, final List<T> items, Function<T, String> func) {
        return join(new StringBuilder(), delimiter, items, func).toString();
    }
}