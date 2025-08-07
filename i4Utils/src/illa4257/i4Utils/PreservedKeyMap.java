package illa4257.i4Utils;

import illa4257.i4Utils.lists.ConvertIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings("NullableProblems")
public class PreservedKeyMap<K, V> extends KeyMap<K, V> {
    private final Map<K, K> originalKeys;

    public PreservedKeyMap(final Map<K, K> keys, final Map<K, V> map, final Function<K, K> keyProcessor, final Function<Object, Object> keyObjectProcessor) {
        super(map, keyProcessor, keyObjectProcessor);
        originalKeys = keys;
        for (final K k : map.keySet())
            originalKeys.put(kp.apply(k), k);
    }

    @Override
    public V put(final K key, final V value) {
        final K k = kp.apply(key);
        originalKeys.put(k, key);
        return originalMap.put(k, value);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            final K k = kp.apply(e.getKey());
            originalKeys.put(k, e.getKey());
            originalMap.put(k, e.getValue());
        }
    }

    @Override
    public void clear() {
        originalMap.clear();
        originalKeys.clear();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new Set<Entry<K, V>>() {
            private final Set<Entry<K, V>> o = originalMap.entrySet();

            @Override
            public int size() {
                return o.size();
            }

            @Override
            public boolean isEmpty() {
                return o.isEmpty();
            }

            @Override
            public boolean contains(final Object o) {
                return this.o.contains(o);
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new ConvertIterator<>(o.iterator(), e -> new Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return originalKeys.getOrDefault(e.getKey(), e.getKey());
                    }

                    @Override
                    public V getValue() {
                        return e.getValue();
                    }

                    @Override
                    public V setValue(final V value) {
                        return e.setValue(value);
                    }
                });
            }

            @Override
            public Object[] toArray() {
                return o.toArray();
            }

            @Override
            public <T> T[] toArray(final T[] a) {
                return o.toArray(a);
            }

            @Override
            public boolean add(final Entry<K, V> kvEntry) {
                return o.add(kvEntry);
            }

            @Override
            public boolean remove(final Object o) {
                return this.o.remove(o);
            }

            @Override
            public boolean containsAll(final Collection<?> c) {
                return o.containsAll(c);
            }

            @Override
            public boolean addAll(final Collection<? extends Entry<K, V>> c) {
                return o.addAll(c);
            }

            @Override
            public boolean retainAll(final Collection<?> c) {
                return o.retainAll(c);
            }

            @Override
            public boolean removeAll(final Collection<?> c) {
                return o.removeAll(c);
            }

            @Override
            public void clear() {
                o.clear();
            }
        };
    }
}
