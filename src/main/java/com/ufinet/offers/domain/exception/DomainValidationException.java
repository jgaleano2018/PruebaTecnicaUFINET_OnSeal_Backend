package com.ufinet.offers.domain.exception;

import java.util.List;

/**
 * Uno o varios datos de la oferta incumplen las reglas del negocio.
 */
public class DomainValidationException extends DomainException {

    private final transient List<ValidationError> errors;

    public DomainValidationException(List<ValidationError> errors) {
        super("La oferta contiene datos inválidos");
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
