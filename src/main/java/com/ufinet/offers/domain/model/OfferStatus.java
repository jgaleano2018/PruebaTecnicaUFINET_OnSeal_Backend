package com.ufinet.offers.domain.model;

/**
 * Ciclo de vida de una oferta: nace en BORRADOR y, al presentarse,
 * pasa a PRESENTADA o NO_VIABLE (estados finales).
 */
public enum OfferStatus {
    BORRADOR,
    PRESENTADA,
    NO_VIABLE
}
