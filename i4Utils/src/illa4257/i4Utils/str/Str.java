package illa4257.i4Utils.str;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
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

    private static final ConcurrentLinkedQueue<StringBuilder> builders = new ConcurrentLinkedQueue<>();

    static {
        ArrayList<Character> l = new ArrayList<>();
        for (final char c : STR_NUMS.toCharArray())
            l.add(c);
        CHARS_NUMS = Collections.unmodifiableList(l);
    }

    public static StringBuilder builder() {
        final StringBuilder b = builders.poll();
        return b != null ? b : new StringBuilder();
    }

    public static void recycle(final StringBuilder builder) {
        builder.setLength(0);
        builders.offer(builder);
    }

    public static String encodeURI(final String uri, final boolean isQuery) throws UnsupportedEncodingException {
        final String space = isQuery ? "+" : "%20";
        final StringBuilder builder = builder();
        try {
            for (final char ch : uri.toCharArray())
                if (STR_URI_CHARS.indexOf(ch) == -1)
                    builder.append(URLEncoder.encode(String.valueOf(ch), UTF_8));
                else if (ch == ' ')
                    builder.append(space);
                else
                    builder.append(ch);
            return builder.toString();
        } finally {
            recycle(builder);
        }
    }

    public static StringBuilder repeat(final StringBuilder builder, final String str, int n) {
        for (; n > 0; n--)
            builder.append(str);
        return builder;
    }

    public static String repeat(final String str, final int n) {
        final StringBuilder b = builder();
        try {
            return repeat(b, str, n).toString();
        } finally {
            recycle(b);
        }
    }

    public static <T> StringBuilder join(final StringBuilder builder, final CharSequence delimiter, final Iterator<T> iter, final Function<T, CharSequence> func) {
        if (iter == null)
            return builder;
        if (!iter.hasNext())
            return builder;
        builder.append(func.apply(iter.next()));
        while (iter.hasNext())
            builder.append(delimiter).append(func.apply(iter.next()));
        return builder;
    }

    public static <T> StringBuilder join(final StringBuilder builder, final CharSequence delimiter, final Iterator<T> iter, final BiConsumer<T, StringBuilder> func) {
        if (iter == null)
            return builder;
        if (!iter.hasNext())
            return builder;
        func.accept(iter.next(), builder);
        while (iter.hasNext())
            func.accept(iter.next(), builder.append(delimiter));
        return builder;
    }

    public static <T> StringBuilder join(final StringBuilder builder, final CharSequence delimiter, final Iterable<T> items, final Function<T, CharSequence> func) {
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

    public static <T> StringBuilder join(final StringBuilder builder, final CharSequence delimiter, final Iterable<T> items, final BiConsumer<T, StringBuilder> func) {
        if (items == null)
            return builder;
        final Iterator<T> iter = items.iterator();
        if (!iter.hasNext())
            return builder;
        func.accept(iter.next(), builder);
        while (iter.hasNext())
            func.accept(iter.next(), builder.append(delimiter));
        return builder;
    }

    public static <T> String join(final CharSequence delimiter, final Iterator<T> items, final Function<T, CharSequence> func) {
        final StringBuilder b = builder();
        try {
            return join(b, delimiter, items, func).toString();
        } finally {
            recycle(b);
        }
    }

    public static <T> String join(final CharSequence delimiter, final Iterator<T> items, final BiConsumer<T, StringBuilder> func) {
        final StringBuilder b = builder();
        try {
            return join(b, delimiter, items, func).toString();
        } finally {
            recycle(b);
        }
    }

    public static <T> String join(final CharSequence delimiter, final Iterable<T> items, final Function<T, CharSequence> func) {
        final StringBuilder b = builder();
        try {
            return join(b, delimiter, items, func).toString();
        } finally {
            recycle(b);
        }
    }

    public static <T> String join(final CharSequence delimiter, final Iterable<T> items, final BiConsumer<T, StringBuilder> func) {
        final StringBuilder b = builder();
        try {
            return join(b, delimiter, items, func).toString();
        } finally {
            recycle(b);
        }
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