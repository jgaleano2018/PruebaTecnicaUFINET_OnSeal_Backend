package com.ufinet.offers.application.service;

import com.ufinet.offers.application.port.in.CreateOfferCommand;
import com.ufinet.offers.application.port.in.CreateOfferUseCase;
import com.ufinet.offers.application.port.in.GetOfferUseCase;
import com.ufinet.offers.application.port.in.ListOffersUseCase;
import com.ufinet.offers.application.port.in.PresentOfferUseCase;
import com.ufinet.offers.application.port.out.OfferRepositoryPort;
import com.ufinet.offers.domain.exception.OfferNotFoundException;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.service.OfferFactory;
import com.ufinet.offers.domain.service.PresentationPolicy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Orquesta los casos de uso de ofertas. No contiene reglas de negocio:
 * las delega en los servicios de dominio y persiste a través del puerto de salida.
 */
public class OfferApplicationService
        implements CreateOfferUseCase, ListOffersUseCase, GetOfferUseCase, PresentOfferUseCase {

    private final OfferRepositoryPort offerRepository;
    private final OfferFactory offerFactory;
    private final PresentationPolicy presentationPolicy;

    public OfferApplicationService(OfferRepositoryPort offerRepository,
                                   OfferFactory offerFactory,
                                   PresentationPolicy presentationPolicy) {
        this.offerRepository = Objects.requireNonNull(offerRepository, "offerRepository");
        this.offerFactory = Objects.requireNonNull(offerFactory, "offerFactory");
        this.presentationPolicy = Objects.requireNonNull(presentationPolicy, "presentationPolicy");
    }

    @Override
    public Mono<Offer> create(CreateOfferCommand command) {
        return Mono.fromCallable(() -> offerFactory.createDraft(command.client(), command.services()))
                .flatMap(offerRepository::save);
    }

    @Override
    public Flux<Offer> listAll() {
        return offerRepository.findAll();
    }

    @Override
    public Mono<Offer> getById(Long id) {
        return offerRepository.findById(id)
                .switchIfEmpty(Mono.error(() -> new OfferNotFoundException(id)));
    }

    @Override
    public Mono<Offer> present(Long id) {
        return getById(id)
                .map(presentationPolicy::present)
                .flatMap(offerRepository::save);
    }
}
