package com.ufinet.offers.application.port.in;

import com.ufinet.offers.domain.model.Offer;
import reactor.core.publisher.Mono;

/** Aplica la regla de presentación a una oferta en BORRADOR. */
public interface PresentOfferUseCase {

    Mono<Offer> present(Long id);
}
