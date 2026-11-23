package com.ufinet.offers.application.port.in;

import com.ufinet.offers.domain.model.Offer;
import reactor.core.publisher.Mono;

/** Obtiene una oferta por id; emite OfferNotFoundException si no existe. */
public interface GetOfferUseCase {

    Mono<Offer> getById(Long id);
}
