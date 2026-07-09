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
}