package illa4257.i4Framework.base.utils;

import illa4257.i4Framework.base.graphics.Context;
import illa4257.i4Framework.base.graphics.ContextRecorder;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.IPath;
import illa4257.i4Framework.base.graphics.Paint;
import illa4257.i4Framework.base.styling.Orientation;
import illa4257.i4Framework.base.styling.StyleProperty;
import illa4257.i4Utils.logger.i4Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

public abstract class SVGParser {
    public static final i4Logger L = new i4Logger("SVGParser").registerHandler(i4Logger.INSTANCE);

    public final Context context;
    private byte state = 0;

    private final StringBuilder text = new StringBuilder();

    public final Object rootTransform;

    public SVGParser(final Context context) {
        this.context = context;
        rootTransform = context.cloneTransform();
    }

    protected abstract boolean hasNextChar() throws IOException;
    protected abstract char nextChar() throws IOException;

    private class Layer {
        public final String node;
        public float x, y, width, height, strokeWidth = 1;
        public Paint stroke, fill;
        public String d;
        public Object transform;

        public Layer(final String node) {
            this.node = node;
            if (layer == null)
                return;
            x = layer.x;
            y = layer.y;
            width = layer.width;
            height = layer.height;
            strokeWidth = layer.strokeWidth;
            stroke = layer.stroke;
            fill = layer.fill;
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

    private float calc(final String value, final Orientation orientation) throws IOException {
        if (value.endsWith("%")) {
            final Layer p = layers.peek();
            if (p == null)
                throw new IOException("Invalid root value " + value);
            final float v = Float.parseFloat(value.substring(0, value.length() - 1)) / 100;
            return orientation == Orientation.HORIZONTAL ?
                    v * p.width :
                    v * p.height;
        }
        return Float.parseFloat(value);
    }

    private void attrNode(final String attr, final String value) throws IOException {
        switch (attr.toLowerCase()) {
            case "version":
            case "xmlns":
                break;
            case "x":
                layer.x = calc(value, Orientation.HORIZONTAL);
                break;
            case "y":
                layer.y = calc(value, Orientation.VERTICAL);
                break;
            case "stroke-width":
                layer.strokeWidth = Float.parseFloat(value);
                break;
            case "width":
                layer.width = calc(value, Orientation.HORIZONTAL);
                break;
            case "height":
                layer.height = calc(value, Orientation.VERTICAL);
                break;
            case "fill":
                layer.fill = StyleProperty.toPaint(value, null);
                break;
            case "stroke":
                layer.stroke = StyleProperty.toPaint(value, null);
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

    private void finishNode() {
        layer.transform = context.cloneTransform();
        switch (node.toLowerCase()) {
            case "svg":
                if (context instanceof ContextRecorder) {
                    final ContextRecorder r = (ContextRecorder) context;
                    r.width = layer.width;
                    r.height = layer.height;
                } else
                    context.scale(1f / layer.width, 1f / layer.height);
                break;
            case "rect":
                context.translate(layer.x, layer.y);
                if (should(layer.stroke)) {
                    context.setPaint(layer.stroke);
                    context.setStrokeWidth(layer.strokeWidth);
                    context.drawRect(0, 0, layer.width, layer.height);
                }
                if (should(layer.fill)) {
                    context.setPaint(layer.fill);
                    context.fillRect(0, 0, layer.width, layer.height);
                }
                break;
            case "path":
                final boolean s = should(layer.stroke), f = should(layer.fill);
                if (layer.d == null || (!s && !f))
                    break;
                text.setLength(0);
                final IPath p = context.newPath();
                byte state = 0;
                char op = ' ';
                float n1 = 0, n2 = 0;
                for (final char ch : layer.d.toCharArray()) {
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
                if (should(layer.stroke)) {
                    context.setPaint(layer.stroke);
                    context.setStrokeWidth(layer.strokeWidth);
                    context.draw(p);
                }
                if (should(layer.fill)) {
                    context.setPaint(layer.fill);
                    context.fill(p);
                }
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
        context.setTransform(layer.transform);
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
            context.setTransform(rootTransform);
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