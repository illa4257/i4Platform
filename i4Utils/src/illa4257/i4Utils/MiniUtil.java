package illa4257.i4Utils;

import illa4257.i4Utils.logger.Level;
import illa4257.i4Utils.logger.i4Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;

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
}