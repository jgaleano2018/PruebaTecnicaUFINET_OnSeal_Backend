package com.ufinet.offers.infrastructure.adapter.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El plazo solo puede ser 12, 24 o 36 meses. Un valor nulo se considera válido
 * (la obligatoriedad se valida con {@code @NotNull}).
 */
@Documented
@Constraint(validatedBy = ContractTermValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidContractTerm {

    String message() default "El plazo debe ser 12, 24 o 36 meses";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
