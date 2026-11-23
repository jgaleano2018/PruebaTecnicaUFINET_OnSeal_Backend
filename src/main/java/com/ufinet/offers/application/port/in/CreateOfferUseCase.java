package com.ufinet.offers.application.port.in;

import com.ufinet.offers.domain.model.Offer;
import reactor.core.publisher.Mono;

/** Crea una oferta en BORRADOR calculando el MRC de cada servicio. */
public interface CreateOfferUseCase {

    Mono<Offer> create(CreateOfferCommand command);
}
