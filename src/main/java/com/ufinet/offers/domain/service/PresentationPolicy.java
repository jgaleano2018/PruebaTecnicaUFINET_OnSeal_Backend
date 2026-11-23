package com.ufinet.offers.domain.service;

import com.ufinet.offers.domain.exception.InvalidOfferStateException;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;

import java.util.Objects;

/**
 * Regla de presentación.
 * <ul>
 *     <li>Si algún servicio tiene viable = false, la oferta pasa a NO_VIABLE.</li>
 *     <li>Si todos son viables, pasa a PRESENTADA.</li>
 *     <li>Si ya no está en BORRADOR, se rechaza (409) y no cambia nada.</li>
 * </ul>
 */
public class PresentationPolicy {

    public Offer present(Offer offer) {
        Objects.requireNonNull(offer, "offer");
        if (!offer.isDraft()) {
            throw new InvalidOfferStateException(offer.getId(), offer.getStatus());
        }
        return offer.withStatus(resolveTargetStatus(offer));
    }

    OfferStatus resolveTargetStatus(Offer offer) {
        return offer.allServicesViable() ? OfferStatus.PRESENTADA : OfferStatus.NO_VIABLE;
    }
}
