package com.ufinet.offers.application.port.out;

import com.ufinet.offers.domain.model.Offer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para persistir ofertas. El dominio no conoce la tecnología
 * (JPA/Hibernate) que lo implementa.
 */
public interface OfferRepositoryPort {

    Mono<Offer> save(Offer offer);

    Mono<Offer> findById(Long id);

    Flux<Offer> findAll();
}
