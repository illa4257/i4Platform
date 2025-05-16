package illa4257.i4Utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("NullableProblems")
public class KeyMap<K, V> implements Map<K, V> {
    private final Map<K, V> m;
    private final Function<K, K> kp;
    private final Function<Object, Object> kop;

    public KeyMap(final Map<K, V> map, final Function<K, K> keyProcessor, final Function<Object, Object> keyObjectProcessor) {
        m = map;
        kp = keyProcessor;
        kop = keyObjectProcessor;
    }

    public KeyMap(final Map<K, V> map, final Map<K, V> copyFrom, final Function<K, K> keyProcessor, final Function<Object, Object> keyObjectProcessor) {
        m = map;
        kp = keyProcessor;
        kop = keyObjectProcessor;
        putAll(copyFrom);
    }

    @Override public int size() { return m.size(); }
    @Override public boolean isEmpty() { return m.isEmpty(); }
    @Override public boolean containsKey(final Object key) { return m.containsKey(kop.apply(key)); }
    @Override public boolean containsValue(final Object value) { return m.containsValue(value); }
    @Override public V get(final Object key) { return m.get(kop.apply(key)); }
    @Override public V put(final K key, final V value) { return m.put(kp.apply(key), value); }
    @Override public V remove(final Object key) { return m.remove(kop.apply(key)); }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> e : map.entrySet())
            m.put(kp.apply(e.getKey()), e.getValue());
    }

    @Override public void clear() { m.clear(); }
    @Override public Set<K> keySet() { return m.keySet(); }
    @Override public Collection<V> values() { return m.values(); }
    @Override public Set<Entry<K, V>> entrySet() { return m.entrySet(); }
    @Override public V getOrDefault(final Object key, final V defaultValue) { return m.getOrDefault(kop.apply(key), defaultValue); }
    @Override public void forEach(final BiConsumer<? super K, ? super V> action) { m.forEach(action); }
    @Override public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) { m.replaceAll(function); }
    @Override public V putIfAbsent(final K key, final V value) { return m.putIfAbsent(kp.apply(key), value); }
    @Override public boolean remove(final Object key, final Object value) { return m.remove(kop.apply(key), value); }
    @Override public boolean replace(final K key, final V oldValue, final V newValue) { return m.replace(kp.apply(key), oldValue, newValue); }
    @Override public V replace(final K key, final V value) { return m.replace(kp.apply(key), value); }
    @Override public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) { return m.computeIfAbsent(kp.apply(key), mappingFunction); }
    @Override public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return m.computeIfPresent(kp.apply(key), remappingFunction); }
    @Override public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return m.compute(kp.apply(key), remappingFunction); }
    @Override public V merge(final K key, V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) { return m.merge(kp.apply(key), value, remappingFunction); }
    @Override public String toString() { return m.toString(); }
}