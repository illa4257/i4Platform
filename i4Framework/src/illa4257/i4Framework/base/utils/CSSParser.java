package illa4257.i4Framework.base.utils;

import illa4257.i4Framework.base.styling.Style;
import illa4257.i4Framework.base.styling.StyleSelector;
import illa4257.i4Framework.base.styling.Stylesheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Stack;

public class CSSParser {
    public final Stylesheet stylesheet;
    public final Reader reader;

    public static final byte
            STATE_PROPERTY_NAME = 0,
            STATE_PROPERTY_NAME_END = 1,
            STATE_PROPERTY_VALUE = 2,
            STATE_SELECTOR = 3,
            STATE_ID = 4,
            STATE_CLASS = 5,
            STATE_PSEUDO_CLASS = 6,
            STATE_SINGLE_QUOTE = 126,
            STATE_DOUBLE_QUOTE = 127;

    public final StringBuilder builder = new StringBuilder();
    public final Style root = new Style();
    public ArrayList<StyleSelector> selectors = new ArrayList<>(), snapshot = new ArrayList<>();
    public String propertyName = null;
    public StyleSelector selector = null;
    public Style style = null;
    public boolean special = false, comment = false, nextSelector = false;
    public byte state = STATE_PROPERTY_NAME, altState;
    public final Stack<Nest> stack = new Stack<>();

    public class Nest {
        public final ArrayList<StyleSelector> selectors;
        public final Style style;

        public Nest() {
            //this.selector = CSSParser.this.selector;
            this.style = CSSParser.this.style;
            this.selectors = new ArrayList<>(CSSParser.this.selectors);
            CSSParser.this.selectors.clear();
        }

        public void apply() {
            CSSParser.this.selector = null;
            CSSParser.this.style = style;
            CSSParser.this.selectors.clear();
            CSSParser.this.selectors.addAll(this.selectors);
        }
    }

    public CSSParser(final Stylesheet stylesheet, final Reader reader) {
        this.stylesheet = stylesheet;
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        final StyleSelector sel = new StyleSelector();
        sel.tag.set("*");
        stylesheet.add(sel, root);
    }

    public static void parse(final Stylesheet stylesheet, final Reader reader) throws IOException {
        new CSSParser(stylesheet, reader).parse();
    }

    public void parse() throws IOException {
        int ch;
        while ((ch = reader.read()) != -1) {
            if (comment) {
                if (ch == '/' && special) {
                    comment = special = false;
                    continue;
                }
                if (ch == '*') {
                    special = true;
                    continue;
                }
                special = false;
                continue;
            }
            if (state != STATE_SINGLE_QUOTE && state != STATE_DOUBLE_QUOTE) {
                if (ch == '*' && special) {
                    comment = true;
                    special = false;
                    continue;
                }
                if (ch == '/') {
                    if (!special) {
                        special = true;
                        continue;
                    }
                } else if (special) {
                    special = false;
                    handle('/');
                }
            }
            handle((char) ch);
        }
    }

    public void handle(final char ch) {
        switch (state) {
            case STATE_PROPERTY_NAME:
                switch (ch) {
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        if (builder.length() > 0) {
                            builder.append(' ');
                            state = STATE_PROPERTY_NAME_END;
                        }
                        break;
                    case ':':
                        if (builder.length() == 0) {
                            state = STATE_PSEUDO_CLASS;
                            break;
                        }
                        propertyName = builder.toString();
                        builder.setLength(0);
                        state = STATE_PROPERTY_VALUE;
                        break;
                    case '.':
                    case ',':
                    case '>':
                    {
                        stack.push(new Nest());
                        state = STATE_SELECTOR;
                        selector = new StyleSelector();
                        final char[] v = builder.toString().toCharArray();
                        builder.setLength(0);
                        for (final char c : v)
                            handle(c);
                        handle(ch);
                        break;
                    }
                    case '{': {
                        stack.push(new Nest());
                        state = STATE_SELECTOR;
                        selector = new StyleSelector();
                        final char[] v = builder.toString().toCharArray();
                        builder.setLength(0);
                        for (final char c : v)
                            handle(c);
                        handle('{');
                        break;
                    }
                    case '}':
                        builder.setLength(0);
                        final Nest nest = stack.pop();
                        nest.apply();
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                break;
            case STATE_PROPERTY_NAME_END:
                switch (ch) {
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        break;
                    case '>': {
                        stack.push(new Nest());
                        state = STATE_SELECTOR;
                        selector = new StyleSelector();
                        final char[] v = builder.toString().toCharArray();
                        builder.setLength(0);
                        for (final char c : v)
                            handle(c);
                        handle('>');
                        break;
                    }
                    case '{': {
                        stack.push(new Nest());
                        state = STATE_SELECTOR;
                        selector = new StyleSelector();
                        final char[] v = builder.toString().toCharArray();
                        builder.setLength(0);
                        for (final char c : v)
                            handle(c);
                        handle('{');
                        break;
                    }
                    default:
                        throw new RuntimeException("Unknown char in name end " + ch + " | " + builder);
                }
                break;
            case STATE_PROPERTY_VALUE:
                switch (ch) {
                    case ';':
                        final Style s = style != null ? style : root;
                        s.set(propertyName, builder.toString());
                        builder.setLength(0);
                        state = STATE_PROPERTY_NAME;
                        break;
                    case '{':
                        stack.push(new Nest());
                        state = STATE_SELECTOR;
                        selector = new StyleSelector();
                        final char[] v = builder.toString().toCharArray();
                        builder.setLength(0);
                        for (final char c : propertyName.toCharArray())
                            handle(c);
                        handle(':');
                        for (final char c : v)
                            handle(c);
                        handle('{');
                        break;
                    case '\'':
                        altState = state;
                        state = STATE_SINGLE_QUOTE;
                        builder.append('\'');
                        break;
                    case '"':
                        altState = state;
                        state = STATE_DOUBLE_QUOTE;
                        builder.append('"');
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                break;
            case STATE_SINGLE_QUOTE:
                switch (ch) {
                    case '\'':
                        builder.append('\'');
                        if (special) {
                            special = false;
                            break;
                        }
                        state = altState;
                        break;
                    case '\\':
                        if (!special) {
                            special = true;
                            builder.append('\\');
                            break;
                        }
                        break;
                    default:
                        special = false;
                        builder.append(ch);
                        break;
                }
                break;
            case STATE_DOUBLE_QUOTE:
                switch (ch) {
                    case '"':
                        builder.append('"');
                        if (special) {
                            special = false;
                            break;
                        }
                        state = altState;
                        break;
                    case '\\':
                        if (!special) {
                            special = true;
                            builder.append('\\');
                            break;
                        }
                        break;
                    default:
                        special = false;
                        builder.append(ch);
                        break;
                }
                break;
            case STATE_SELECTOR:
            case STATE_ID:
            case STATE_CLASS:
            case STATE_PSEUDO_CLASS:
                switch (ch) {
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        if (selector.isEmpty())
                            break;
                        nextSelector = true;
                        break;
                    case '>':
                        setSelector();
                        if (!selector.isEmpty())
                            selector = new StyleSelector(selector);
                        nextSelector = false;
                        state = STATE_SELECTOR;
                        break;
                    case ',':
                        setSelector();
                        selectors.add(selector);
                        selector = new StyleSelector();
                        nextSelector = false;
                        state = STATE_SELECTOR;
                        break;
                    case '#':
                        setSelector();
                        state = STATE_ID;
                        break;
                    case '.':
                        setSelector();
                        state = STATE_CLASS;
                        break;
                    case ':':
                        setSelector();
                        state = STATE_PSEUDO_CLASS;
                        break;
                    case '{':
                        setSelector();
                        if (!selector.isEmpty())
                            selectors.add(selector);
                        if (selectors.isEmpty())
                            style = stack.peek().style;
                        else {
                            style = new Style();
                            final Nest n = stack.peek();
                            if (!n.selectors.isEmpty()) {
                                final ArrayList<StyleSelector> cur = selectors;
                                selectors = snapshot;
                                snapshot = cur;
                                for (final StyleSelector sel : snapshot)
                                    for (final StyleSelector parent : n.selectors) {
                                        final StyleSelector ns = sel.clone();
                                        ns.getFirstParent().parent = parent;
                                        selectors.add(ns);
                                        stylesheet.add(ns, style);
                                    }
                                snapshot.clear();
                            } else
                                for (final StyleSelector sel : selectors)
                                    stylesheet.add(sel, style);
                        }

                        selector = null;
                        state = STATE_PROPERTY_NAME;
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                break;
            default:
                throw new RuntimeException("Unknown state " + state);
        }
    }

    private void setSelector() {
        switch (state) {
            case STATE_SELECTOR:
                selector.tag.set(builder.toString().trim());
                break;
            case STATE_ID:
                selector.id.set(builder.toString().trim());
                break;
            case STATE_CLASS:
                selector.classes.offer(builder.toString().trim());
                break;
            case STATE_PSEUDO_CLASS:
                selector.pseudoClasses.offer(builder.toString().trim());
                break;
            default:
                throw new RuntimeException("Unknown state " + state);
        }
        builder.setLength(0);
    }
}