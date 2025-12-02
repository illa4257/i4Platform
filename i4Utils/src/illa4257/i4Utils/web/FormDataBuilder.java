package illa4257.i4Utils.web;

import illa4257.i4Utils.MiniUtil;
import illa4257.i4Utils.str.Str;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FormDataBuilder {
    public final Map<String, String> map;

    public FormDataBuilder() { map = new HashMap<>(); }

    public FormDataBuilder put(final String key, final String value) {
        map.put(key, value);
        return this;
    }

    public FormDataBuilder putAll(final Map<? extends String, ? extends String> map) {
        this.map.putAll(map);
        return this;
    }

    public FormDataBuilder putAll(final Object... pairs) {
        MiniUtil.put(map, pairs);
        return this;
    }

    public byte[] toBytes() { return toString().getBytes(StandardCharsets.UTF_8); }

    @Override
    public String toString() {
        return Str.join("&", map.entrySet(), (e, b) -> {
            try {
                b.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
            } catch (final UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}