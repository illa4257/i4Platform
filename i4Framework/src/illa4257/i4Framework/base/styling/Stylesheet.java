package illa4257.i4Framework.base.styling;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Stylesheet {
    public final ConcurrentLinkedQueue<Map.Entry<StyleSelector, Style>> stylesheet = new ConcurrentLinkedQueue<>();

    public void clear() {
        stylesheet.clear();
    }

    public void add(final StyleSelector selector, final Style style) {
        stylesheet.offer(new AbstractMap.SimpleImmutableEntry<>(selector, style));
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        for (final Map.Entry<StyleSelector, Style> e : stylesheet) {
            b.append(e.getKey()).append(" {\r\n");
            for (final StyleProperty p : e.getValue().properties)
                b.append("\t").append(p.name).append(": ").append(p.objs).append(";\r\n");
            b.append("}\r\n");
        }
        return b.toString();
    }
}