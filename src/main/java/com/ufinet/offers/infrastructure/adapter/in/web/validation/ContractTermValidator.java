package com.ufinet.offers.infrastructure.adapter.in.web.validation;

import com.ufinet.offers.domain.model.ContractTerm;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContractTermValidator implements ConstraintValidator<ValidContractTerm, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || ContractTerm.isAllowed(value);
    }
}
