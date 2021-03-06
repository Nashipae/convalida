package convalida.compiler.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

import convalida.annotations.Between;
import convalida.annotations.Cnpj;
import convalida.annotations.ConfirmEmail;
import convalida.annotations.ConfirmPassword;
import convalida.annotations.Cpf;
import convalida.annotations.CreditCard;
import convalida.annotations.Email;
import convalida.annotations.FutureDate;
import convalida.annotations.Ipv4;
import convalida.annotations.Ipv6;
import convalida.annotations.Isbn;
import convalida.annotations.Length;
import convalida.annotations.NumericLimit;
import convalida.annotations.OnlyNumber;
import convalida.annotations.Password;
import convalida.annotations.PastDate;
import convalida.annotations.Pattern;
import convalida.annotations.Required;
import convalida.annotations.Url;
import convalida.compiler.model.ValidationClass;
import convalida.compiler.model.ValidationField;

import static convalida.compiler.ProcessingOptions.isDatabindingEnabled;
import static convalida.compiler.util.Constants.ABSTRACT_VALIDATOR;
import static convalida.compiler.util.Constants.BETWEEN_ANNOTATION;
import static convalida.compiler.util.Constants.BETWEEN_VALIDATOR;
import static convalida.compiler.util.Constants.BUTTON;
import static convalida.compiler.util.Constants.CNPJ_ANNOTATION;
import static convalida.compiler.util.Constants.CNPJ_VALIDATOR;
import static convalida.compiler.util.Constants.CONFIRM_EMAIL_VALIDATION;
import static convalida.compiler.util.Constants.CONFIRM_EMAIL_VALIDATOR;
import static convalida.compiler.util.Constants.CONFIRM_PASSWORD_ANNOTATION;
import static convalida.compiler.util.Constants.CONFIRM_PASSWORD_VALIDATOR;
import static convalida.compiler.util.Constants.CONVALIDA_DATABINDING_R;
import static convalida.compiler.util.Constants.CPF_ANNOTATION;
import static convalida.compiler.util.Constants.CPF_VALIDATOR;
import static convalida.compiler.util.Constants.CREDIT_CARD_ANNOTATION;
import static convalida.compiler.util.Constants.CREDIT_CARD_VALIDATOR;
import static convalida.compiler.util.Constants.EMAIL_ANNOTATION;
import static convalida.compiler.util.Constants.EMAIL_VALIDATOR;
import static convalida.compiler.util.Constants.FUTURE_DATE_ANNOTATION;
import static convalida.compiler.util.Constants.FUTURE_DATE_VALIDATOR;
import static convalida.compiler.util.Constants.IPV4_ANNOTATION;
import static convalida.compiler.util.Constants.IPV4_VALIDATOR;
import static convalida.compiler.util.Constants.IPV6_ANNOTATION;
import static convalida.compiler.util.Constants.IPV6_VALIDATOR;
import static convalida.compiler.util.Constants.ISBN_ANNOTATION;
import static convalida.compiler.util.Constants.ISBN_VALIDATOR;
import static convalida.compiler.util.Constants.LENGTH_ANNOTATION;
import static convalida.compiler.util.Constants.LENGTH_VALIDATOR;
import static convalida.compiler.util.Constants.LIST;
import static convalida.compiler.util.Constants.NON_NULL;
import static convalida.compiler.util.Constants.NUMERIC_LIMIT_ANNOTATION;
import static convalida.compiler.util.Constants.NUMERIC_LIMIT_VALIDATOR;
import static convalida.compiler.util.Constants.ONLY_NUMBER_ANNOTATION;
import static convalida.compiler.util.Constants.ONLY_NUMBER_VALIDATOR;
import static convalida.compiler.util.Constants.OVERRIDE;
import static convalida.compiler.util.Constants.PASSWORD_ANNOTATION;
import static convalida.compiler.util.Constants.PASSWORD_VALIDATOR;
import static convalida.compiler.util.Constants.PAST_DATE_ANNOTATION;
import static convalida.compiler.util.Constants.PAST_DATE_VALIDATOR;
import static convalida.compiler.util.Constants.PATTERN_ANNOTATION;
import static convalida.compiler.util.Constants.PATTERN_VALIDATOR;
import static convalida.compiler.util.Constants.REQUIRED_ANNOTATION;
import static convalida.compiler.util.Constants.REQUIRED_VALIDATOR;
import static convalida.compiler.util.Constants.UI_THREAD;
import static convalida.compiler.util.Constants.URL_ANNOTATION;
import static convalida.compiler.util.Constants.URL_VALIDATOR;
import static convalida.compiler.util.Constants.VALIDATOR_SET;
import static convalida.compiler.util.Constants.VIEW;
import static convalida.compiler.util.Constants.VIEWGROUP;
import static convalida.compiler.util.Constants.VIEW_DATA_BINDING;
import static convalida.compiler.util.Constants.VIEW_ONCLICK_LISTENER;
import static convalida.compiler.util.Constants.VIEW_TAG_UTILS;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * @author Wellington Costa on 19/06/2017.
 */
public class JavaFiler {

    public static JavaFile cookJava(ValidationClass validationClass) {
        TypeSpec.Builder validationClassBuilder = TypeSpec.classBuilder(validationClass.className)
                .addModifiers(PUBLIC)
                .addField(VALIDATOR_SET, "validatorSet", PRIVATE)
                .addMethod(createValidateOnClickListener(validationClass))
                .addMethod(createClearValidationsOnClickListener(validationClass));

        if(isDatabindingEnabled() && validationClass.fields.isEmpty()) {
            validationClassBuilder
                    .addMethod(createDatabindingConstructor(validationClass))
                    .addMethod(createInitDatabindingMethod(validationClass));
        } else {
            validationClassBuilder
                    .addMethod(createConstructor(validationClass))
                    .addMethod(createInitMethod(validationClass));
        }

        return JavaFile.builder(validationClass.packageName, validationClassBuilder.build())
                .addFileComment("Generated code from Convalida. Do not modify!")
                .build();
    }

    private static MethodSpec createConstructor(ValidationClass validationClass) {
        return MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addModifiers(FINAL)
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addStatement("validatorSet = new $T()", VALIDATOR_SET)
                .addCode(createValidationsCodeBlock(validationClass))
                .addCode(createValidateOnClickCodeBlock(validationClass))
                .addCode(createClearValidationsOnClickCodeBlock(validationClass))
                .build();
    }

    private static MethodSpec createDatabindingConstructor(ValidationClass validationClass) {
        return MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addModifiers(FINAL)
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addParameter(ParameterSpec
                        .builder(VIEW_DATA_BINDING, "binding")
                        .addModifiers(FINAL)
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addCode("if (binding.hasPendingBindings()) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("binding.executePendingBindings();")
                .addCode("\n")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addStatement("validatorSet = new $T()", VALIDATOR_SET)
                .addCode(
                        "$T<$T> views = $T.getViewsByTag(($T) binding.getRoot(), $T.id.validation_type);",
                        LIST,
                        VIEW,
                        VIEW_TAG_UTILS,
                        VIEWGROUP,
                        CONVALIDA_DATABINDING_R
                )
                .addCode("\n")
                .addCode(
                        "$T<$T> buttons = $T.getViewsByTag(($T) binding.getRoot(), $T.id.validation_action);",
                        LIST,
                        VIEW,
                        VIEW_TAG_UTILS,
                        VIEWGROUP,
                        CONVALIDA_DATABINDING_R
                )
                .addCode("\n")
                .addCode("$T validateButton = null;", BUTTON)
                .addCode("\n")
                .addCode("$T clearValidationsButton = null;", BUTTON)
                .addCode("\n")
                .addCode("\n")
                .addCode("for (View view : views) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode(
                        "java.util.List<$T> validators = (java.util.List<$T>) view.getTag($T.id.validation_type);",
                        ABSTRACT_VALIDATOR,
                        ABSTRACT_VALIDATOR,
                        CONVALIDA_DATABINDING_R
                )
                .addCode("\n\n")
                .addCode(
                        "validatorSet.addValidators(validators);",
                        ABSTRACT_VALIDATOR,
                        CONVALIDA_DATABINDING_R
                )
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addCode("for (View button : buttons) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode(
                        "if (button.getTag($T.id.validation_action).equals($T.id.validate) && validateButton == null) {",
                        CONVALIDA_DATABINDING_R,
                        CONVALIDA_DATABINDING_R
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("validateButton = ($T) button;", BUTTON)
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addCode(
                        "if (button.getTag($T.id.validation_action).equals($T.id.clear) && clearValidationsButton == null) {",
                        CONVALIDA_DATABINDING_R,
                        CONVALIDA_DATABINDING_R
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("clearValidationsButton = ($T) button;", BUTTON)
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addCode("if (validateButton != null && clearValidationsButton != null) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("break;")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addCode("if (validateButton != null) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("validateOnClickListener(validateButton, target);")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .addCode("\n")
                .addCode("if (clearValidationsButton != null) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("clearValidationsOnClickListener(clearValidationsButton, target);")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode("\n")
                .build();
    }

    private static CodeBlock createValidationsCodeBlock(final ValidationClass validationClass) {
        CodeBlock.Builder builder = CodeBlock.builder();

        for(ValidationField field : validationClass.fields) {
            switch (field.annotationClassName) {
                case REQUIRED_ANNOTATION:
                    builder.add(createRequiredValidationCodeBlock(field));
                    break;
                case EMAIL_ANNOTATION:
                    builder.add(createEmailValidationCodeBlock(field));
                    break;
                case CONFIRM_EMAIL_VALIDATION:
                    for(ValidationField validationField : validationClass.fields) {
                        if(validationField.annotationClassName.equals(EMAIL_ANNOTATION)) {
                            builder.add(createConfirmEmailValidationCodeBlock(validationField, field));
                            break;
                        }
                    }
                    break;
                case PATTERN_ANNOTATION:
                    builder.add(createPatternValidationCodeBlock(field));
                    break;
                case LENGTH_ANNOTATION:
                    builder.add(createLengthValidationCodeBlock(field));
                    break;
                case ONLY_NUMBER_ANNOTATION:
                    builder.add(createOnlyNumberValidationCodeBlock(field));
                    break;
                case PASSWORD_ANNOTATION:
                    builder.add(createPasswordValidationCodeBlock(field));
                    break;
                case CONFIRM_PASSWORD_ANNOTATION:
                    for(ValidationField validationField : validationClass.fields) {
                        if(validationField.annotationClassName.equals(PASSWORD_ANNOTATION)) {
                            builder.add(createConfirmPasswordValidationCodeBlock(validationField, field));
                            break;
                        }
                    }
                    break;
                case CPF_ANNOTATION:
                    builder.add(createCpfValidationCodeBlock(field));
                    break;
                case CNPJ_ANNOTATION:
                    builder.add(createCnpjValidationCodeBlock(field));
                    break;
                case ISBN_ANNOTATION:
                    builder.add(createIsbnValidationCodeBlock(field));
                    break;
                case BETWEEN_ANNOTATION:
                    builder.add(createBetweenValidationCodeBlock(validationClass, field));
                    break;
                case CREDIT_CARD_ANNOTATION:
                    builder.add(createCreditCardValidationCodeBlock(field));
                    break;
                case NUMERIC_LIMIT_ANNOTATION:
                    builder.add(createNumericLimitValidationCodeBlock(field));
                    break;
                case IPV4_ANNOTATION:
                    builder.add(createIpv4ValidationCodeBlock(field));
                    break;
                case IPV6_ANNOTATION:
                    builder.add(createIpv6ValidationCodeBlock(field));
                    break;
                case URL_ANNOTATION:
                    builder.add(createUrlValidationCodeBlock(field));
                    break;
                case PAST_DATE_ANNOTATION:
                    builder.add(createPastDateValidationCodeBlock(field));
                    break;
                case FUTURE_DATE_ANNOTATION:
                    builder.add(createFutureDateValidationCodeBlock(field));
                    break;
            }
        }
        return builder.build();
    }

    private static String errorMessage(boolean hasErrorMessageResId) {
        return (hasErrorMessageResId ? "target.getString($L)" : "$S");
    }

    private static CodeBlock createRequiredValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Required> annotation = Required.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        REQUIRED_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss
                )
                .build();
    }

    private static CodeBlock createEmailValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Email> annotation = Email.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        EMAIL_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createConfirmEmailValidationCodeBlock(
            ValidationField emailField,
            ValidationField confirmEmailField
    ) {
        Element element = confirmEmailField.element;
        Class<ConfirmEmail> annotation = ConfirmEmail.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        CONFIRM_EMAIL_VALIDATOR,
                        emailField.name,
                        confirmEmailField.name,
                        hasErrorMessageResId ? confirmEmailField.id.code : errorMessage,
                        confirmEmailField.autoDismiss
                )
                .build();
    }

    private static CodeBlock createPatternValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Pattern> annotation = Pattern.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $S, $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        PATTERN_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.element.getAnnotation(annotation).pattern(),
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createLengthValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Length> annotation = Length.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L, $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        LENGTH_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.element.getAnnotation(annotation).min(),
                        field.element.getAnnotation(annotation).max(),
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createOnlyNumberValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<OnlyNumber> annotation = OnlyNumber.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        ONLY_NUMBER_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createPasswordValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Password> annotation = Password.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $S, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        PASSWORD_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.element.getAnnotation(annotation).min(),
                        field.element.getAnnotation(annotation).pattern(),
                        field.autoDismiss
                )
                .build();
    }

    private static CodeBlock createConfirmPasswordValidationCodeBlock(
            ValidationField passwordField,
            ValidationField confirmPasswordField
    ) {
        Element element = confirmPasswordField.element;
        Class<ConfirmPassword> annotation = ConfirmPassword.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        CONFIRM_PASSWORD_VALIDATOR,
                        passwordField.name,
                        confirmPasswordField.name,
                        hasErrorMessageResId ? confirmPasswordField.id.code : errorMessage,
                        confirmPasswordField.autoDismiss
                )
                .build();
    }

    private static CodeBlock createCpfValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Cpf> annotation = Cpf.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        CPF_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createCnpjValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Cnpj> annotation = Cnpj.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        CNPJ_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createIsbnValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Isbn> annotation = Isbn.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        ISBN_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createBetweenValidationCodeBlock(ValidationClass validationClass, ValidationField startField) {
        ValidationField endField = null;

        for(ValidationField field : validationClass.fields) {
            if(field.annotationClassName.equals(Between.Limit.class.getCanonicalName())) {
                endField = field;
                break;
            }
        }

        if(endField != null) {
            boolean hasStartErrorMessageResId =
                    startField.element.getAnnotation(Between.Start.class).errorMessageResId() != -1;
            String startErrorMessage =
                    startField.element.getAnnotation(Between.Start.class).errorMessage();

            boolean hasEndErrorMessageResId =
                    endField.element.getAnnotation(Between.Limit.class).errorMessageResId() != -1;
            String endErrorMessage =
                    endField.element.getAnnotation(Between.Limit.class).errorMessage();

            String block = "validatorSet.addValidator(new $T(target.$N, target.$N, " +
                    errorMessage(hasStartErrorMessageResId) +
                    ", " +
                    errorMessage(hasEndErrorMessageResId) +
                    ", $L, $L))";

            return CodeBlock.builder()
                    .addStatement(
                            block,
                            BETWEEN_VALIDATOR,
                            startField.name,
                            endField.name,
                            hasStartErrorMessageResId ? startField.id.code : startErrorMessage,
                            hasEndErrorMessageResId ? endField.id.code : endErrorMessage,
                            startField.autoDismiss,
                            endField.autoDismiss
                    )
                    .build();
        } else return CodeBlock.builder().build();
    }

    private static CodeBlock createCreditCardValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<CreditCard> annotation = CreditCard.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        CREDIT_CARD_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createNumericLimitValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<NumericLimit> annotation = NumericLimit.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $S, $S, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        NUMERIC_LIMIT_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).min(),
                        field.element.getAnnotation(annotation).max(),
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createIpv4ValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Ipv4> annotation = Ipv4.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        IPV4_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createIpv6ValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Ipv6> annotation = Ipv6.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        IPV6_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createUrlValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<Url> annotation = Url.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        URL_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createPastDateValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<PastDate> annotation = PastDate.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $S, $S, $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        PAST_DATE_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.element.getAnnotation(annotation).dateFormat(),
                        field.element.getAnnotation(annotation).limitDate(),
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createFutureDateValidationCodeBlock(ValidationField field) {
        Element element = field.element;
        Class<FutureDate> annotation = FutureDate.class;
        boolean hasErrorMessageResId = element.getAnnotation(annotation).errorMessageResId() != -1;
        String errorMessage = element.getAnnotation(annotation).errorMessage();
        String block = "validatorSet.addValidator(new $T(target.$N, " +
                errorMessage(hasErrorMessageResId) +
                ", $S, $S, $L, $L))";

        return CodeBlock.builder()
                .addStatement(
                        block,
                        FUTURE_DATE_VALIDATOR,
                        field.name,
                        hasErrorMessageResId ? field.id.code : errorMessage,
                        field.element.getAnnotation(annotation).dateFormat(),
                        field.element.getAnnotation(annotation).limitDate(),
                        field.autoDismiss,
                        field.element.getAnnotation(annotation).required()
                )
                .build();
    }

    private static CodeBlock createValidateOnClickCodeBlock(ValidationClass validationClass) {
        Element button = validationClass.getValidateButton();
        if(button != null) {
            return CodeBlock.builder()
                    .add("\n")
                    .add(
                            "validateOnClickListener(target.$N, target);",
                            button.getSimpleName().toString()
                    )
                    .build();
        }
        return CodeBlock.of("");
    }

    private static CodeBlock createClearValidationsOnClickCodeBlock(ValidationClass validationClass) {
        Element button = validationClass.getClearValidationsButton();
        if (button != null) {
            return CodeBlock.builder()
                    .add("\n")
                    .add(
                            "clearValidationsOnClickListener(target.$N, target);",
                            button.getSimpleName().toString()
                    )
                    .add("\n")
                    .build();
        }
        return CodeBlock.of("");
    }

    private static MethodSpec createValidateOnClickListener(
            ValidationClass validationClass) {
        Element onValidationErrorMethod = validationClass.getOnValidationErrorMethod();
        MethodSpec.Builder method = MethodSpec.methodBuilder("validateOnClickListener")
                .addModifiers(PRIVATE)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(BUTTON, "button")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addModifiers(FINAL)
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addCode(
                        "button.setOnClickListener(new $T() {",
                        VIEW_ONCLICK_LISTENER
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode(
                        "@$T public void onClick($T view) {",
                        OVERRIDE,
                        VIEW
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("if(validatorSet.isValid()) {")
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode(
                        "target.$N();",
                        validationClass.getOnValidationSuccessMethod().getSimpleName()
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("}");

        if(onValidationErrorMethod != null) {
            boolean hasOneParam = ((ExecutableElement) onValidationErrorMethod)
                    .getParameters()
                    .size() == 1;

            String callMethodCodeBlock = hasOneParam ?
                    "target.$N(validatorSet.errors);" :
                    "target.$N();";

            method.addCode(" else {")
                    .addCode("\n")
                    .addCode(CodeBlock.builder().indent().build())
                    .addCode(
                            callMethodCodeBlock,
                            validationClass.getOnValidationErrorMethod().getSimpleName()
                    )
                    .addCode("\n")
                    .addCode(CodeBlock.builder().unindent().build())
                    .addCode("}");
        }

        method.addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("}")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("\n")
                .addCode("});")
                .addCode("\n");

        return method.build();
    }

    private static MethodSpec createClearValidationsOnClickListener(
            ValidationClass validationClass) {
        return MethodSpec.methodBuilder("clearValidationsOnClickListener")
                .addModifiers(PRIVATE)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(BUTTON, "button")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addCode(
                        "button.setOnClickListener(new $T() {",
                        VIEW_ONCLICK_LISTENER
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode(
                        "@$T public void onClick($T view) {",
                        OVERRIDE,
                        VIEW
                )
                .addCode("\n")
                .addCode(CodeBlock.builder().indent().build())
                .addCode("validatorSet.clearValidators();")
                .addCode("\n")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("}")
                .addCode("\n")
                .addCode(CodeBlock.builder().unindent().build())
                .addCode("});")
                .addCode("\n")
                .build();
    }

    private static MethodSpec createInitMethod(ValidationClass validationClass) {
        ClassName className = ClassName.get(validationClass.packageName, validationClass.className);
        return MethodSpec.methodBuilder("init")
                .addModifiers(PUBLIC, STATIC)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addStatement("new $T(target)", className)
                .build();
    }

    private static MethodSpec createInitDatabindingMethod(ValidationClass validationClass) {
        ClassName className = ClassName.get(validationClass.packageName, validationClass.className);
        return MethodSpec.methodBuilder("init")
                .addModifiers(PUBLIC, STATIC)
                .addAnnotation(UI_THREAD)
                .addParameter(ParameterSpec
                        .builder(validationClass.typeName, "target")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addParameter(ParameterSpec
                        .builder(VIEW_DATA_BINDING, "binding")
                        .addAnnotation(NON_NULL)
                        .build()
                )
                .addStatement("new $T(target, binding)", className)
                .build();
    }

}