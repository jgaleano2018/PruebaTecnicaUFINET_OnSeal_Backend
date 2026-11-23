package com.ufinet.offers.domain.exception;

/**
 * Error de validación asociado a un campo (por ejemplo "servicios[0].mbps").
 */
public record ValidationError(String field, String message) {
}
