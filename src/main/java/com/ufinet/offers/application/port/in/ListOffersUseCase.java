package com.ufinet.offers.application.port.in;

import com.ufinet.offers.domain.model.Offer;
import reactor.core.publisher.Flux;

/** Lista todas las ofertas. */
public interface ListOffersUseCase {

    Flux<Offer> listAll();
}
