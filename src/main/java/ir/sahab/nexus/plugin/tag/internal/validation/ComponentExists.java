package ir.sahab.nexus.plugin.tag.internal.validation;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({TYPE, PARAMETER, METHOD, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ComponentExistsValidator.class)
@Documented
public @interface ComponentExists {

    String message() default "Associated component does not exists";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
