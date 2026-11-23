package com.ufinet.offers.domain.exception;

/**
 * La oferta solicitada no existe.
 */
public class OfferNotFoundException extends DomainException {

    private final Long offerId;

    public OfferNotFoundException(Long offerId) {
        super("No existe una oferta con id %d".formatted(offerId));
        this.offerId = offerId;
    }

    public Long getOfferId() {
        return offerId;
    }
}
