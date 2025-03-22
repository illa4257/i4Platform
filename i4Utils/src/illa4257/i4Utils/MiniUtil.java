package illa4257.i4Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
}