package illa4257.i4Framework.base.styling;

import illa4257.i4Framework.base.utils.Cache;
import illa4257.i4Framework.base.graphics.Color;
import illa4257.i4Framework.base.graphics.Image;
import illa4257.i4Utils.runnables.Provider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class StyleSetting {
    public final ConcurrentHashMap<Class<?>, Object> values = new ConcurrentHashMap<>();

    public StyleSetting() {}
    public StyleSetting(final int value) { values.put(Integer.class, value); }
    public StyleSetting(final String value) { values.put(String.class, value); }
    public StyleSetting(final Color value) { values.put(Color.class, value); }

    public <T> T get(final Class<T> type) { return (T) values.get(type); }

    public <T> T get(final Class<T> type, final T alt) {
        final T r = get(type);
        return r != null ? r : alt;
    }

    public <T> T getP(final Class<T> type, final Provider<T> alt) {
        final T r = get(type);
        return r != null ? r : alt.run();
    }

    public <T> T getF(final Class<T> type, final Function<StyleSetting, T> alt) {
        final T r = get(type);
        return r != null ? r : alt.apply(this);
    }

    public <T> StyleSetting set(final Class<T> type, final T newValue) {
        values.clear();
        values.put(type, newValue);
        return this;
    }

    public <T> T computeIfAbsent(final Class<T> type, final T computeValue) {
        return (T) values.computeIfAbsent(type, k -> computeValue);
    }

    public <T> T computeIfAbsentP(final Class<T> type, final Provider<T> computeFunction) {
        return (T) values.computeIfAbsent(type, k -> computeFunction.run());
    }

    public <T> T computeIfAbsentP(final Class<T> type, final Provider<T> computeFunction, final T defaultValue) {
        final T r = (T) values.computeIfAbsent(type, k -> computeFunction.run());
        return r != null ? computeFunction.run() : defaultValue;
    }

    public <T> T computeIfAbsentF(final Class<T> type, final Function<StyleSetting, T> computeFunction) {
        return (T) values.computeIfAbsent(type, k -> computeFunction.apply(this));
    }

    public <T> T computeIfAbsentF(final Class<T> type, final Function<StyleSetting, T> computeFunction, final T defaultValue) {
        final T r = (T) values.computeIfAbsent(type, k -> computeFunction.apply(this));
        return r != null ? r : defaultValue;
    }

    public StyleSetting set(final int value) { return set(Integer.class, value); }
    public StyleSetting set(final String value) { return set(String.class, value); }
    public StyleSetting set(final Color value) { return set(Color.class, value); }

    public Color color() { return computeIfAbsentF(Color.class, Color::styleSettingParser); }

    public Color color(final Color defaultColor) {
        return computeIfAbsentF(Color.class, Color::styleSettingParser, defaultColor);
    }

    public Cursor cursor() { return computeIfAbsentF(Cursor.class, Cursor::from); }

    public Image image(final Image defaultImage) {
        return computeIfAbsentF(Image.class, k -> {
            final String value = k.get(String.class);
            if (value == null)
                return null;
            return Cache.images.get(value);
        }, defaultImage);
    }

    public StyleNumber number(final StyleNumber defaultValue) {
        return computeIfAbsentF(StyleNumber.class, StyleNumber::styleSettingParser, defaultValue);
    }

    @Override
    public String toString() {
        return "StyleSetting" + values;
    }
}