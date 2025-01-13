package illa4257.i4Framework.base;

import java.io.IOException;
import java.io.Reader;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class CSSParser {
    public static void parse(final ConcurrentHashMap<StyleSelector, ConcurrentHashMap<String, StyleSetting>> stylesheet,
                             final Reader reader) throws IOException {
        final ArrayList<StyleSelector> selectors = new ArrayList<>();
        int ch;
        while (true) {
            ch = reader.read();
            if (ch == -1)
                break;
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n')
                continue;
            if (ch == '{')
                throw new UnexpectedException("Unknown character " + ch);
            final StyleSelector selector = new StyleSelector();
            if (ch == '*') {
                do {
                    ch = reader.read();
                    if (ch == -1)
                        throw new IOException("End of reader");
                } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
            }
            if (ch != '#' && ch != '.' && ch != ',' && ch != '{') {
                final StringBuilder tag = new StringBuilder();
                while (true) {
                    tag.append((char) ch);
                    ch = reader.read();
                    if (ch == -1)
                        throw new IOException("End of reader");
                    if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' ||
                            ch == '#' || ch == '.' || ch == ',' || ch == '{') {
                        selector.tag.set(tag.toString());
                        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                        }
                        break;
                    }
                }
            }
            while (true) {
                if (ch == '#') {
                    final StringBuilder id = new StringBuilder();
                    while (true) {
                        ch = reader.read();
                        if (ch == -1)
                            throw new IOException("End of reader");
                        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '{' || ch == '.' || ch == '#') {
                            while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                                ch = reader.read();
                                if (ch == -1)
                                    throw new IOException("End of reader");
                            }
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
                        ch = reader.read();
                        if (ch == -1)
                            throw new IOException("End of reader");
                        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == '{' || ch == '.' || ch == '#' || ch == ':') {
                            while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                                ch = reader.read();
                                if (ch == -1)
                                    throw new IOException("End of reader");
                            }
                            break;
                        }
                        cls.append((char) ch);
                    }
                    selector.addClass(cls.toString());
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
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                        } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ';');
                        if (ch == '}')
                            break;
                        final StringBuilder name = new StringBuilder();
                        name.append((char) ch);
                        while (true) {
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n' || ch == ';' || ch == ':')
                                break;
                            name.append((char) ch);
                        }
                        while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                        }
                        if (ch != ':')
                            continue;
                        do {
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                        } while (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
                        final StringBuilder value = new StringBuilder();
                        while (ch != ';') {
                            value.append((char) ch);
                            ch = reader.read();
                            if (ch == -1)
                                throw new IOException("End of reader");
                        }
                        if (name.length() == 0 || value.length() == 0)
                            continue;
                        style.put(name.toString(), new StyleSetting(value.toString()));
                    }
                    for (final StyleSelector s : selectors)
                        stylesheet.put(s, style);
                    selectors.clear();
                    break;
                }
                throw new UnexpectedException("Unknown character " + ch);
            }
        }
    }
}