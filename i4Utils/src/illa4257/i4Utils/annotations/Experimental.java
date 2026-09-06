package illa4257.i4Utils.annotations;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Export
public @interface Experimental {
    String value() default "A subject to change in the next versions.";
}