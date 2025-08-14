package illa4257.i4Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class Str {
    public static final String
            STR_NUMS = "0123456789",
            STR_EN_LOW = "abcdefghijklmnopqrstuvwxyz",
            STR_EN_UP = STR_EN_LOW.toUpperCase(),
            UTF_8 = StandardCharsets.UTF_8.toString(),
            STR_URI_CHARS = STR_EN_LOW + STR_EN_UP + STR_NUMS + ":/?#[]@!$&'()*+,;=-._~%"
    ;

    public static final List<Character>
            CHARS_NUMS;

    static {
        ArrayList<Character> l = new ArrayList<>();
        for (final char c : STR_NUMS.toCharArray())
            l.add(c);
        CHARS_NUMS = Collections.unmodifiableList(l);
    }

    public static String encodeURI(final String uri, final boolean isQuery) throws UnsupportedEncodingException {
        final String space = isQuery ? "+" : "%20";
        final StringBuilder builder = new StringBuilder();

        for (final char ch : uri.toCharArray())
            if (STR_URI_CHARS.indexOf(ch) == -1)
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

    public static <T> StringBuilder join(final StringBuilder builder, final String delimiter, final Iterator<T> iter, Function<T, String> func) {
        if (iter == null)
            return builder;
        if (!iter.hasNext())
            return builder;
        builder.append(func.apply(iter.next()));
        while (iter.hasNext())
            builder.append(delimiter).append(func.apply(iter.next()));
        return builder;
    }

    public static <T> StringBuilder join(final StringBuilder builder, final String delimiter, final Iterable<T> items, Function<T, String> func) {
        if (items == null)
            return builder;
        final Iterator<T> iter = items.iterator();
        if (!iter.hasNext())
            return builder;
        builder.append(func.apply(iter.next()));
        while (iter.hasNext())
            builder.append(delimiter).append(func.apply(iter.next()));
        return builder;
    }

    public static <T> String join(final String delimiter, final Iterable<T> items, Function<T, String> func) {
        return join(new StringBuilder(), delimiter, items, func).toString();
    }

    public static String random(final int len) { return random(new Random(), len, STR_NUMS + STR_EN_LOW + STR_EN_UP); }

    public static String random(final Random randomizer, int len, final String chars) {
        final int cl = chars.length();
        char[] buf = new char[len];
        for (len--; len >= 0; len--)
            buf[len] = chars.charAt(randomizer.nextInt(cl));
        return new String(buf);
    }

    public static int countSubstringsNoOverlap(final String str, final String substring) {
        int i = 0, n = 0;
        while ((i = str.indexOf(substring, i)) != -1) {
            i += substring.length();
            n++;
        }
        return n;
    }

    public static boolean contains(final String str, final Iterable<String> iterable) {
        for (final String s : iterable)
            if (str.contains(s))
                return true;
        return false;
    }

    public static boolean containsChars(final String str, final Iterable<Character> chars) {
        for (final char c : chars)
            if (str.indexOf(c) != -1)
                return true;
        return false;
    }
}