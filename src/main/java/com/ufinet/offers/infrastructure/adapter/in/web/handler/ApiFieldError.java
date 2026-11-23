package com.ufinet.offers.infrastructure.adapter.in.web.handler;

/**
 * Error de un campo concreto dentro de la propiedad "errors" del ProblemDetail.
 */
public record ApiFieldError(String field, String message) {
}
