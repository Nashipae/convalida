package convalida.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * @author Wellington Costa on 19/01/2018.
 */
@Target(FIELD)
@Retention(CLASS)
public @interface ValidateOnClick { }