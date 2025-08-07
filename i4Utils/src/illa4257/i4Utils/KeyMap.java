package illa4257.i4Utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("NullableProblems")
public class KeyMap<K, V> implements Map<K, V> {
    public final Map<K, V> originalMap;
    protected final Function<K, K> kp;
    protected final Function<Object, Object> kop;

    public KeyMap(final Map<K, V> map, final Function<K, K> keyProcessor, final Function<Object, Object> keyObjectProcessor) {
        originalMap = map;
        kp = keyProcessor;
        kop = keyObjectProcessor;
    }

    @Override public int size() { return originalMap.size(); }
    @Override public boolean isEmpty() { return originalMap.isEmpty(); }
    @Override public boolean containsKey(final Object key) { return originalMap.containsKey(kop.apply(key)); }
    @Override public boolean containsValue(final Object value) { return originalMap.containsValue(value); }
    @Override public V get(final Object key) { return originalMap.get(kop.apply(key)); }
    @Override public V put(final K key, final V value) { return originalMap.put(kp.apply(key), value); }
    @Override public V remove(final Object key) { return originalMap.remove(kop.apply(key)); }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        for (final Map.Entry<? extends K, ? extends V> e : map.entrySet())
            originalMap.put(kp.apply(e.getKey()), e.getValue());
    }

    @Override public void clear() { originalMap.clear(); }
    @Override public Set<K> keySet() { return originalMap.keySet(); }
    @Override public Collection<V> values() { return originalMap.values(); }
    @Override public Set<Entry<K, V>> entrySet() { return originalMap.entrySet(); }
    @Override public V getOrDefault(final Object key, final V defaultValue) { return originalMap.getOrDefault(kop.apply(key), defaultValue); }
    @Override public void forEach(final BiConsumer<? super K, ? super V> action) { originalMap.forEach(action); }
    @Override public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) { originalMap.replaceAll(function); }
    @Override public V putIfAbsent(final K key, final V value) { return originalMap.putIfAbsent(kp.apply(key), value); }
    @Override public boolean remove(final Object key, final Object value) { return originalMap.remove(kop.apply(key), value); }
    @Override public boolean replace(final K key, final V oldValue, final V newValue) { return originalMap.replace(kp.apply(key), oldValue, newValue); }
    @Override public V replace(final K key, final V value) { return originalMap.replace(kp.apply(key), value); }
    @Override public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) { return originalMap.computeIfAbsent(kp.apply(key), mappingFunction); }
    @Override public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return originalMap.computeIfPresent(kp.apply(key), remappingFunction); }
    @Override public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return originalMap.compute(kp.apply(key), remappingFunction); }
    @Override public V merge(final K key, V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) { return originalMap.merge(kp.apply(key), value, remappingFunction); }
    @Override public String toString() { return originalMap.toString(); }
}