package illa4257.i4Framework.base.utils;

import illa4257.i4Utils.media.Image;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class Cache {
    public static final ConcurrentHashMap<String, Image> images = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Integer, WeakReference<Image>> scaled = new ConcurrentHashMap<>();

    public static Image scale(final Image img, final float width, final float height) {
        if (img.width == width && img.height == height)
            return img;
        final int w = Math.round(width), h = Math.round(height), id = Objects.hash(img, w, h);
        final WeakReference<Image> ref = scaled.get(id);
        final Image re = ref != null ? ref.get() : null;
        if (re != null)
            return re;
        final AtomicReference<Image> r = new AtomicReference<>();
        scaled.compute(id, (ignored1, ignored2) -> {
            final Image i = img.scale(w, h);
            r.set(i);
            return new WeakReference<>(i);
        });
        return r.get();
    }
}