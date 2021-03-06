package convalida.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import androidx.annotation.StringRes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * @author Wellington Costa on 07/02/18.
 */
@Target(FIELD)
@Retention(SOURCE)
public @interface ConfirmEmail {

    @StringRes int errorMessageResId() default -1;

    String errorMessage() default "";

    boolean autoDismiss() default true;

}
