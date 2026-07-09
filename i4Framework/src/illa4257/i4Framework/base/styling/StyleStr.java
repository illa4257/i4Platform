package illa4257.i4Framework.base.styling;

import java.util.function.Predicate;

public class StyleStr {
    public String value;

    public StyleStr(final String value) {
        this.value = value;
    }

    public static final Predicate<Object> filter = obj -> obj instanceof StyleStr;

    @Override
    public String toString() {
        return '"' + value + '"';
    }
}