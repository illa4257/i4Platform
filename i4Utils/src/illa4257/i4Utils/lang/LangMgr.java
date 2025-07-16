package illa4257.i4Utils.lang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LangMgr {
    public final ConcurrentHashMap<String, LangString> items = new ConcurrentHashMap<>();

    public LangString of(final String key) {
        return items.computeIfAbsent(key, LangString::new);
    }

    public LangMgr put(final String key, final String value) {
        items.computeIfAbsent(key, ignored -> new LangString()).value = value;
        return this;
    }

    public LangMgr putIfAbsent(final String key, final String value) {
        final LangString s = items.computeIfAbsent(key, ignored -> new LangString());
        if (s.value == null || s.value.equals(key))
            s.value = value;
        return this;
    }

    public LangMgr reset() {
        for (final Map.Entry<String, LangString> e : items.entrySet())
            e.getValue().value = e.getKey();
        return this;
    }
}