package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.graphics.Color;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Style {
    public final ConcurrentLinkedDeque<StyleProperty> properties = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedQueue<Consumer<StyleProperty>> subscribers = new ConcurrentLinkedQueue<>();

    public void set(final String key, final String value) {
        final String k = Objects.requireNonNull(key).toLowerCase();
        final StyleProperty np = StyleProperty.parse(k, value);
        properties.offer(np);
        properties.removeIf(p -> p.name.equals(k) && p != np);
        for (final Consumer<StyleProperty> l : subscribers)
            l.accept(np);
    }

    public void set(final String key, final Color value) {
        final String k = Objects.requireNonNull(key).toLowerCase();
        final StyleProperty np = StyleProperty.parse(k, value.toHexRGBA());
        properties.offer(np);
        properties.removeIf(p -> p.name.equals(k) && p != np);
        for (final Consumer<StyleProperty> l : subscribers)
            l.accept(np);
    }

    public void remove(final String key) {
        final String k = Objects.requireNonNull(key).toLowerCase();
        properties.removeIf(p -> p.name.equals(k));
        /// TODO: properly implement
    }

    public StyleProperty get(final String key) {
        final String k = Objects.requireNonNull(key).toLowerCase();
        final Iterator<StyleProperty> iter = properties.descendingIterator();
        while (iter.hasNext()) {
            final StyleProperty p = iter.next();
            if (p.name.equals(k))
                return p;
        }
        return null;
    }

    public StyleProperty get(final List<String> key) {
        if (key.isEmpty())
            return null;
        final Iterator<StyleProperty> iter = properties.descendingIterator();
        while (iter.hasNext()) {
            final StyleProperty p = iter.next();
            if (key.contains(p.name))
                return p;
        }
        return null;
    }

    public StyleProperty get(final String[] keys) {
        if (keys.length == 0)
            return null;
        final Iterator<StyleProperty> iter = properties.descendingIterator();
        while (iter.hasNext()) {
            final StyleProperty p = iter.next();
            if (Arrays.binarySearch(keys, p.name) >= 0)
                return p;
        }
        return null;
    }

    public void subscribe(final Consumer<StyleProperty> listener) {
        subscribers.offer(listener);
    }

    public void unsubscribe(final Consumer<StyleProperty> listener) {
        subscribers.remove(listener);
    }
}