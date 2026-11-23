package com.ufinet.offers.infrastructure.adapter.in.web.handler;

/**
 * Códigos de error estables que acompañan a cada ProblemDetail.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    MALFORMED_REQUEST,
    OFFER_NOT_FOUND,
    INVALID_OFFER_STATE,
    CONCURRENT_MODIFICATION,
    HTTP_ERROR,
    INTERNAL_ERROR
}
