package illa4257.i4Framework.base.utils;

import illa4257.i4Framework.base.styling.StyleSelector;
import illa4257.i4Framework.base.styling.StyleSetting;

import java.io.IOException;
import java.io.Reader;
import java.rmi.UnexpectedException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CSSParser {
    private static int r(final Reader reader) throws IOException {
        int ch = reader.read();
        if (ch == '/' && reader.markSupported()) {
            reader.mark(1);
            ch = reader.read();
            if (ch == '*') {
                m:
                while (true) {
                    ch = reader.read();
                    if (ch == -1)
                        throw new IOException("End of reader");
                    while (ch == '*') {
                        ch = reader.read();
                        if (ch == '/')
                            break m;
                        if (ch == -1)
                            throw new IOException("End of reader");
                    }
                }
                return r(reader);
            }
            reader.reset();
        }
        return ch;
    }

    private static char re(final Reader reader) throws IOException {
        int ch = r(reader);
        if (ch == -1)
            throw new IOException("End of reader");
        return (char) ch;
    }

    /**
     * Parses CSS from reader and puts all results into provided stylesheet.
     *
     * @param stylesheet All parsed results will be stored here, it will not delete parsed results if it fails.<br>
     *                   And it will not delete old styles.
     * @param reader If your reader is not supporting marks, but you need to skip comments, use {@link java.io.BufferedReader}.
     * @throws IOException If reading is fails, or reached end of reader.
     * @throws UnexpectedException If unexpected characters are encountered during parsing.
     */
    public static void parse(final ConcurrentLinkedQueue<Map.Entry<StyleSelector, ConcurrentHashMap<String, StyleSetting>>> stylesheet,
                             final Reader reader) throws IOException {
        final ArrayList<StyleSelector> selectors = new ArrayList<>();
        int ch;
        while (true) {
            ch = r(reader);
            if (ch == -1)
                break;
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                continue;
            if (ch == '{')
                throw new UnexpectedException("Unknown character " + (char) ch + " / " + ch);
            final StyleSelector selector = new StyleSelector();
            if (ch == '*') {
                do {
                    ch = re(reader);
                } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
            }
            if (ch != '#' && ch != '.' && ch != ':' && ch != ',' && ch != '{') {
                final StringBuilder tag = new StringBuilder();
                while (true) {
                    tag.append((char) ch);
                    ch = re(reader);
                    if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' ||
                            ch == '#' || ch == '.' || ch == ':' || ch == ',' || ch == '{') {
                        selector.tag.set(tag.toString());
                        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                            ch = re(reader);
                        break;
                    }
                }
            }
            while (true) {
                if (ch == '#') {
                    final StringBuilder id = new StringBuilder();
                    while (true) {
                        ch = re(reader);
                        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '{' || ch == ',' || ch == '.' || ch == '#' || ch == ':') {
                            while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                                ch = re(reader);
                            break;
                        }
                        id.append((char) ch);
                    }
                    selector.setID(id.toString());
                    continue;
                }
                if (ch == '.') {
                    final StringBuilder cls = new StringBuilder();
                    while (true) {
                        ch = re(reader);
                        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '{' || ch == ',' || ch == '.' || ch == '#' || ch == ':') {
                            while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                                ch = re(reader);
                            break;
                        }
                        cls.append((char) ch);
                    }
                    selector.addClass(cls.toString());
                    continue;
                }
                if (ch == ':') {
                    final StringBuilder cls = new StringBuilder();
                    while (true) {
                        ch = re(reader);
                        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '{' || ch == ',' || ch == '.' || ch == '#' || ch == ':') {
                            while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                                ch = re(reader);
                            break;
                        }
                        cls.append((char) ch);
                    }
                    selector.addPseudoClass(cls.toString());
                    continue;
                }
                if (ch == ',') {
                    selectors.add(selector);
                    break;
                }
                if (ch == '{') {
                    selectors.add(selector);
                    final ConcurrentHashMap<String, StyleSetting> style = new ConcurrentHashMap<>();
                    while (true) {
                        do {
                            ch = re(reader);
                        } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ';');
                        if (ch == '}')
                            break;
                        final StringBuilder name = new StringBuilder();
                        name.append((char) ch);
                        while (true) {
                            ch = re(reader);
                            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ';' || ch == ':')
                                break;
                            name.append((char) ch);
                        }
                        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                            ch = re(reader);
                        if (ch != ':')
                            continue;
                        do
                            ch = re(reader);
                        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
                        final StringBuilder value = new StringBuilder();
                        while (ch != ';') {
                            value.append((char) ch);
                            ch = re(reader);
                        }
                        if (name.length() == 0 || value.length() == 0)
                            continue;
                        style.put(name.toString(), new StyleSetting(value.toString()));
                    }
                    for (final StyleSelector s : selectors)
                        stylesheet.add(new AbstractMap.SimpleImmutableEntry<>(s, style));
                    selectors.clear();
                    break;
                }
                throw new UnexpectedException("Unknown character " + (char) ch + " / " + ch);
            }
        }
    }
}