package com.ufinet.offers.domain.exception;

/**
 * Excepción base de las reglas de negocio.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
