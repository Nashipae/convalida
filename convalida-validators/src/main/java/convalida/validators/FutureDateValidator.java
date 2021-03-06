package convalida.validators;

import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Wellington Costa on 31/07/2019.
 */
public class FutureDateValidator extends AbstractValidator {

    private final SimpleDateFormat dateFormat;
    private final String limitDate;
    private final boolean required;

    public FutureDateValidator(
            EditText editText,
            String errorMessage,
            String dateFormat,
            String limitDate,
            boolean autoDismiss,
            boolean required
    ) {
        super(editText, errorMessage, autoDismiss);

        this.dateFormat = new SimpleDateFormat(dateFormat, Locale.getDefault());
        this.limitDate = limitDate;
        this.required = required;
    }

    @Override public boolean isValid(String value) {

        if(required && value.isEmpty()) return false;

        try {

            Date current = dateFormat.parse(value);

            Date limit = dateFormat.parse(this.limitDate);

            return beforeOrEqualsTo(current, limit);

        } catch (Exception exception) {

            exception.printStackTrace();

            return false;

        }

    }

    private boolean beforeOrEqualsTo(Date current, Date limit) {
        return current.equals(limit) || current.before(limit);
    }
}
