package illa4257.i4Framework.base.utils;

import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.graphics.ContextRecorder;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.PropIter;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public abstract class SVGParser {
    public static final i4Logger L = new i4Logger("SVGParser").registerHandler(i4Logger.INSTANCE);
    private static final List<String> strokeColor = Arrays.asList("stroke-color", "stroke"),
            strokeWidth = Arrays.asList("stroke-width", "stroke");

    public final Context context;
    private byte state = 0;

    private final StringBuilder text = new StringBuilder();

    public SVGParser(final Context context) {
        this.context = context;
    }

    protected abstract boolean hasNextChar() throws IOException;
    protected abstract char nextChar() throws IOException;

    private class Layer {
        public final String node;
        public String x, y, width, height, strokeWidth;
        public float fx, fy, fWidth, fHeight, fStrokeWidth;
        public String stroke, fill;
        public Paint cFill, cStroke;
        public String d;

        public Layer(final String node) {
            this.node = node;
            if (layer == null)
                return;
            x = layer.x;
            fx = layer.fx;
            y = layer.y;
            fy = layer.fy;
            width = layer.width;
            fWidth = layer.fWidth;
            height = layer.height;
            fHeight = layer.fHeight;
            strokeWidth = layer.strokeWidth;
            fStrokeWidth = layer.fStrokeWidth;
            stroke = layer.stroke;
            cStroke = layer.cStroke;
            fill = layer.fill;
            cFill = layer.cFill;
        }
    }

    private final Stack<Layer> layers = new Stack<>();
    private Layer layer = null;
    private String node = null, s = null;

    private void enterNode(final String node) throws IOException {
        if (layer == null) {
            if (!node.equalsIgnoreCase("svg"))
                throw new IOException("Invalid root node " + node);
            layer = new Layer("svg");
            return;
        }
        switch (node.toLowerCase()) {
            case "rect":
            case "path":
                layers.push(layer);
                layer = new Layer(node.toLowerCase());
                break;
            default:
                throw new IOException("Unknown node " + node);
        }
    }

    private float calc(final String value, final float parent) throws IOException {
        if (value.endsWith("%")) {
            final Layer p = layers.peek();
            if (p == null)
                throw new IOException("Invalid root value " + value);
            return Float.parseFloat(value.substring(0, value.length() - 1)) / 100 * parent;
        }
        return Float.parseFloat(value);
    }

    private float noCalc(final String value) {
        try {
            return Float.parseFloat(value);
        } catch (final Exception ex) {
            return 0;
        }
    }

    private void attrNode(final String attr, final String value) throws IOException {
        switch (attr.toLowerCase()) {
            case "version":
            case "xmlns":
                break;
            case "x":
                layer.x = value;
                layer.fx = noCalc(value);
                break;
            case "y":
                layer.y = value;
                layer.fy = noCalc(value);
                break;
            case "stroke-width":
                layer.strokeWidth = value;
                layer.fStrokeWidth = noCalc(value);
                break;
            case "width":
                layer.width = value;
                layer.fWidth = noCalc(value);
                break;
            case "height":
                layer.height = value;
                layer.fHeight = noCalc(value);
                break;
            case "fill":
                layer.fill = value;
                layer.cFill = StyleProperty.toPaint(value, null);
                break;
            case "stroke":
                layer.stroke = value;
                layer.cStroke = StyleProperty.toPaint(value, null);
                break;
            case "d":
                layer.d = value;
                break;
            default:
                L.d("Unknown attr", attr, "=", value);
                break;
        }
    }

    private static boolean should(final Paint paint) {
        return paint != null && (!(paint instanceof Color) || !(((Color) paint).alpha <= 0));
    }

    private static boolean should(final String s) {
        return s != null && !s.isEmpty();
    }

    private void finishNode() {
        final Layer l = layer;
        context.save();
        switch (node.toLowerCase()) {
            case "svg":
                if (context instanceof ContextRecorder) {
                    final ContextRecorder r = (ContextRecorder) context;
                    r.width = Float.parseFloat(l.width);
                    r.height = Float.parseFloat(l.height);
                } else
                    context.scale(1f / Float.parseFloat(l.width), 1f / Float.parseFloat(l.height));
                break;
            case "rect":
                if (!should(l.stroke) && !should(l.fill))
                    break;
                context.with(c -> {
                    c.translate(l.fx, l.fy);
                    final PropIter pi = c.getPropIter();
                    if (pi == null) {
                        if (should(l.cStroke)) {
                            c.setPaint(l.cStroke);
                            c.setStrokeWidth(l.fStrokeWidth);
                            c.drawRect(0, 0, layer.fWidth, l.fHeight);
                        }
                        if (should(l.cFill)) {
                            c.setPaint(l.cFill);
                            c.fillRect(0, 0, l.fWidth, l.fHeight);
                        }
                        return;
                    }
                    if (should(l.stroke))
                        pi.select(StyleProperty.parse("stroke-color", l.stroke), StyleProperty.paintFilter);
                    else
                        pi.select(strokeColor, StyleProperty.paintFilter);
                    pi.nextLayer().nextSet();
                    final Paint stroke = pi.paint(null);
                    if (should(stroke)) {
                        c.setPaint(stroke);
                        if (should(l.strokeWidth))
                            pi.select(StyleProperty.parse("stroke-width", l.strokeWidth), StyleProperty.pxFilter);
                        else
                            pi.select(strokeWidth, StyleProperty.pxFilter);
                        pi.nextLayer().nextSet();
                        c.setStrokeWidth(pi.f(1, 1));
                        c.drawRect(0, 0, l.fWidth, l.fHeight);
                    }
                    if (should(l.fill))
                        pi.select(StyleProperty.parse("fill", l.fill), StyleProperty.paintFilter);
                    else
                        pi.select("fill", StyleProperty.paintFilter);
                    pi.nextLayer().nextSet();
                    final Paint fill = pi.paint(null);
                    if (should(fill)) {
                        c.setPaint(fill);
                        c.fillRect(0, 0, l.fWidth, l.fHeight);
                    }
                });
                break;
            case "path":
                final boolean s = should(l.stroke), f = should(l.fill);
                if (l.d == null || (!s && !f))
                    break;
                text.setLength(0);
                final IPath p = context.newPath();
                byte state = 0;
                char op = ' ';
                float n1 = 0, n2 = 0;
                for (final char ch : l.d.toCharArray()) {
                    boolean update = true;
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case ',':
                            if (state == 0 || text.length() == 0)
                                continue;
                            update = false;
                        case 'H':
                        case 'h':
                        case 'V':
                        case 'v':
                        case 'L':
                        case 'l':
                        case 'M':
                        case 'm':
                        case 'Z':
                            switch (state) {
                                case 0:
                                    break;
                                case 1:
                                    n1 = Float.parseFloat(text.toString());
                                    switch (op) {
                                        case 'H':
                                            p.lineTo(n1, p.y());
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'h':
                                            p.lineTo(p.x() + n1, p.y());
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'V':
                                            p.lineTo(p.x(), n1);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'v':
                                            p.lineTo(p.x(), p.y() + n1);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        default:
                                            state = 2;
                                            break;
                                    }
                                    break;
                                case 2:
                                    n2 = Float.parseFloat(text.toString());
                                    switch (op) {
                                        case 'M':
                                            p.moveTo(n1, n2);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'm':
                                            p.moveTo(p.x() + n1, p.y() + n2);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'L':
                                            p.lineTo(n1, n2);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        case 'l':
                                            p.lineTo(p.x() + n1, p.y() + n2);
                                            op = ' ';
                                            state = 0;
                                            break;
                                        default:
                                            throw new RuntimeException("Unknown next state");
                                    }
                                    break;
                                default:
                                    throw new RuntimeException("Invalid state " + state);
                            }
                            text.setLength(0);
                            if (update) {
                                if (ch == 'Z') {
                                    p.close();
                                    op = ' ';
                                    state = 0;
                                    break;
                                }
                                op = ch;
                                state = 1;
                            }
                            break;
                        case '.':
                            if (text.indexOf(".") != -1)
                                throw new RuntimeException("Double dots are not allowed for numbers");
                        case '-':
                            if (ch == '-' && text.length() > 0) {
                                throw new RuntimeException("Invalid placement of the negative number");
                            }
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                            if (state == 0)
                                throw new RuntimeException("Invalid state for numbers");
                            text.append(ch);
                            break;
                        default:
                            throw new RuntimeException("Unknown char " + ch);
                    }
                }
                if (!should(l.stroke) && !should(l.fill))
                    break;
                context.with(c -> {
                    c.translate(l.fx, l.fy);
                    final PropIter pi = c.getPropIter();
                    if (pi == null) {
                        if (should(l.cStroke)) {
                            c.setPaint(l.cStroke);
                            c.setStrokeWidth(l.fStrokeWidth);
                            c.draw(p);
                        }
                        if (should(l.cFill)) {
                            c.setPaint(l.cFill);
                            c.fill(p);
                        }
                        return;
                    }
                    if (should(l.stroke))
                        pi.select(StyleProperty.parse("stroke-color", l.stroke), StyleProperty.paintFilter);
                    else
                        pi.select(strokeColor, StyleProperty.paintFilter);
                    pi.nextLayer().nextSet();
                    final Paint stroke = pi.paint(null);
                    if (should(stroke)) {
                        c.setPaint(stroke);
                        if (should(l.strokeWidth))
                            pi.select(StyleProperty.parse("stroke-width", l.strokeWidth), StyleProperty.pxFilter);
                        else
                            pi.select(strokeWidth, StyleProperty.pxFilter);
                        pi.nextLayer().nextSet();
                        c.setStrokeWidth(pi.f(1, 1));
                        c.draw(p);
                    }
                    if (should(l.fill))
                        pi.select(StyleProperty.parse("fill", l.fill), StyleProperty.paintFilter);
                    else
                        pi.select("fill", StyleProperty.paintFilter);
                    pi.nextLayer().nextSet();
                    final Paint fill = pi.paint(null);
                    if (should(fill)) {
                        c.setPaint(fill);
                        c.fill(p);
                    }
                });
                break;
            default:
                L.d("Unknown finish node " + node);
                break;
        }
        text.setLength(0);
    }

    private void exitNode(final String node) throws IOException {
        if (layer == null)
            throw new IOException("No more layers");
        if (!layer.node.equalsIgnoreCase(node))
            throw new IOException("Wrong layer " + layer.node + ", " + node);
        context.restore();
        if (layers.isEmpty()) {
            layer = null;
            return;
        }
        layer = layers.pop();
    }

    protected void parse() throws IOException {
        while (hasNextChar()) {
            final char ch = nextChar();
            switch (state) {
                case 0:
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            break;
                        case '<':
                            state = 1;
                            break;
                        default:
                            throw new IOException("Unknown char " + ch);
                    }
                    break;
                case 1:
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            if (text.length() == 0)
                                break;
                            node = text.toString();
                            enterNode(node);
                            text.setLength(0);
                            state = 2;
                            break;
                        case '/':
                            if (text.length() > 0) {
                                node = text.toString();
                                enterNode(node);
                                finishNode();
                                exitNode(node);
                                state = 5;
                                break;
                            }
                            state = 6;
                            break;
                        case '>':
                            enterNode(text.toString());
                            finishNode();
                            text.setLength(0);
                            state = 0;
                            break;
                        default:
                            text.append(ch);
                            break;
                    }
                    break;
                case 2:
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            if (text.length() == 0)
                                break;
                            attrNode(text.toString(), null);
                            text.setLength(0);
                            break;
                        case '/':
                            state = 5;
                            if (text.length() == 0) {
                                finishNode();
                                exitNode(node);
                                break;
                            }
                            attrNode(text.toString(), null);
                            finishNode();
                            exitNode(node);
                            text.setLength(0);
                            break;
                        case '>':
                            state = 0;
                            if (text.length() == 0) {
                                finishNode();
                                break;
                            }
                            attrNode(text.toString(), null);
                            finishNode();
                            text.setLength(0);
                            break;
                        case '=':
                            if (text.length() == 0)
                                throw new IOException("No attribute value");
                            s = text.toString();
                            text.setLength(0);
                            state = 3;
                            break;
                        default:
                            text.append(ch);
                            break;
                    }
                    break;
                case 3:
                    switch (ch) {
                        case '"':
                            state = 4;
                            break;
                        default:
                            throw new IOException("Unknown char " + ch);
                    }
                    break;
                case 4:
                    switch (ch) {
                        case '"':
                            attrNode(s, text.toString());
                            text.setLength(0);
                            state = 2;
                            break;
                        default:
                            text.append(ch);
                            break;
                    }
                    break;
                case 5:
                    switch (ch) {
                        case '>':
                            state = 0;
                            break;
                        default:
                            throw new IOException("Unknown char " + ch);
                    }
                    break;
                case 6:
                    switch (ch) {
                        case '>':
                            if (text.length() == 0)
                                throw new IOException("Too early closing");
                            exitNode(text.toString());
                            text.setLength(0);
                            state = 0;
                            break;
                        default:
                            text.append(ch);
                            break;
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown state " + state);
            }
        }
    }

    public static ContextRecorder parse(final Reader reader) throws IOException {
        final ContextRecorder r = new ContextRecorder();
        new SVGParserReader(reader, r);
        return r;
    }

    private static class SVGParserReader extends SVGParser {
        private final Reader reader;
        private int next = -1;

        public SVGParserReader(final Reader reader, final Context context) throws IOException {
            super(context);
            this.reader = reader;
            parse();
        }

        @Override
        protected boolean hasNextChar() throws IOException {
            if (next != -1)
                return true;
            next = reader.read();
            return next != -1;
        }

        @Override
        protected char nextChar() {
            final char n = (char) next;
            next = -1;
            return n;
        }
    }
}