package com.ufinet.offers.domain.exception;

import com.ufinet.offers.domain.model.OfferStatus;

/**
 * Se intentó presentar una oferta que ya no está en BORRADOR.
 */
public class InvalidOfferStateException extends DomainException {

    private final Long offerId;
    private final OfferStatus currentStatus;

    public InvalidOfferStateException(Long offerId, OfferStatus currentStatus) {
        super("La oferta %d no se puede presentar porque su estado es %s; solo se pueden presentar ofertas en BORRADOR"
                .formatted(offerId, currentStatus));
        this.offerId = offerId;
        this.currentStatus = currentStatus;
    }

    public Long getOfferId() {
        return offerId;
    }

    public OfferStatus getCurrentStatus() {
        return currentStatus;
    }
}
