package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;

public class MiniUtil {
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T enumValueOfIgnoreCase(final Class<T> enumClass, final String name) throws IllegalAccessException {
        if (name == null)
            throw new NullPointerException("Name is null");
        for (final Field f : enumClass.getFields())
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()) && name.equalsIgnoreCase(f.getName()) && f.getType() == enumClass)
                return (T) f.get(null);
        throw new IllegalArgumentException("No enum constant " + enumClass.getCanonicalName() + "." + name);
    }

    public static File getFile(final Class<?> c) {
        try {
            return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
        } catch (final URISyntaxException ex) {
            i4Logger.INSTANCE.log(Level.WARN, ex);
            return null;
        }
    }

    public static File getPath(final Class<?> c) {
        try {
            return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
        } catch (final URISyntaxException ex) {
            i4Logger.INSTANCE.log(Level.WARN, ex);
            return null;
        }
    }

    public static <L1 extends Iterable<T1>, T1, L2 extends Collection<T2>, T2> L2 convert(final L1 in, final L2 out, final Function<T1, T2> convertor) {
        for (final T1 e : in)
            out.add(convertor.apply(e));
        return out;
    }

    public static <L1 extends Collection<T1>, T1, T2> ArrayList<T2> convert(final L1 in, final Function<T1, T2> convertor) {
        final ArrayList<T2> arr = new ArrayList<>(in.size());
        for (final T1 e : in)
            arr.add(convertor.apply(e));
        return arr;
    }

    public static <L1 extends Iterable<T1>, T1, T2> ArrayList<T2> convert(final L1 in, final Function<T1, T2> convertor) {
        final ArrayList<T2> arr = new ArrayList<>();
        for (final T1 e : in)
            arr.add(convertor.apply(e));
        return arr;
    }

    @SafeVarargs
    public static <T> boolean contains(final T element, final T... array) {
        for (final T e : array)
            if (Objects.equals(element, e))
                return true;
        return false;
    }

    public static <M extends Map<K, V>, K, V> M put(final M map, final Object... pairs) {
        if (pairs.length % 2 != 0)
            throw new IllegalArgumentException("Not even amount of values: " + pairs.length);
        for (int i = 0; i < pairs.length; i++)
            //noinspection unchecked
            map.put((K) pairs[i++], (V) pairs[i]);
        return map;
    }

    public static <T> int indexOf(final T element, final Iterator<T> iterator) {
        int i = 0;
        while (iterator.hasNext()) {
            if (Objects.equals(iterator.next(), element))
                return i;
            i++;
        }
        return -1;
    }

    public static <T> int indexOf(final T element, final Iterable<T> iterable) {
        int i = 0;
        for (final T e : iterable) {
            if (Objects.equals(element, e))
                return i;
            i++;
        }
        return -1;
    }

    @SafeVarargs
    public static <T> int indexOfVarArgs(final T element, T... elements) {
        int i = 0;
        for (final T e : elements) {
            if (Objects.equals(element, e))
                return i;
            i++;
        }
        return -1;
    }

    public static <T> T get(final Iterator<T> iter, final int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        int i = 0;
        for (; i < index; i++)
            if (!iter.hasNext())
                throw new IndexOutOfBoundsException("Index out of range: " + index + ", max: " + i);
            else
                iter.next();
        if (!iter.hasNext())
            throw new IndexOutOfBoundsException("Index out of range: " + index + ", max: " + i);
        return iter.next();
    }

    public static <T> T get(final Iterable<T> iter, final int index) { return get(iter.iterator(), index); }

    public static boolean inRange(final float x, final float y, float range) {
        if (range < 0)
            range = -range;
        final float d = x - y;
        return d < 0 ? -d < range : d < range;
    }

    public static <K, V> K getKeyByValue(final Map<K, V> map, final V value) {
        for (final Map.Entry<K, V> e : map.entrySet())
            if (value.equals(e.getValue()))
                return e.getKey();
        return null;
    }

    public static <K, V> K getKeyF(final Map<K, V> map, final Function<Map.Entry<K, V>, Boolean> cons) {
        for (final Map.Entry<K, V> e : map.entrySet())
            if (cons.apply(e))
                return e.getKey();
        return null;
    }

    public static <K, V> void removeByValue(final Map<K, V> map, final V value) {
        final Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
        while (it.hasNext())
            if (value.equals(it.next().getValue())) {
                it.remove();
                return;
            }
    }
}