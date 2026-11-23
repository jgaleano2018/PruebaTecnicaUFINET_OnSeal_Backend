package com.ufinet.offers.infrastructure.adapter.out.persistence;

import com.ufinet.offers.application.port.out.OfferRepositoryPort;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.infrastructure.adapter.out.persistence.mapper.OfferPersistenceMapper;
import com.ufinet.offers.infrastructure.adapter.out.persistence.repository.SpringDataOfferRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;

/**
 * Adaptador de salida que implementa el puerto con JPA/Hibernate.
 * <p>
 * JPA es bloqueante, por eso cada operación se ejecuta dentro de una transacción
 * en un scheduler elástico dedicado, sin bloquear los hilos del event loop de WebFlux.
 */
@Component
public class OfferPersistenceAdapter implements OfferRepositoryPort {

    private final SpringDataOfferRepository repository;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readTransaction;
    private final Scheduler jpaScheduler;

    public OfferPersistenceAdapter(SpringDataOfferRepository repository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("jpaScheduler") Scheduler jpaScheduler) {
        this.repository = repository;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
        this.jpaScheduler = jpaScheduler;
    }

    @Override
    public Mono<Offer> save(Offer offer) {
        return Mono.fromCallable(() -> writeTransaction.execute(status ->
                        OfferPersistenceMapper.toDomain(repository.saveAndFlush(OfferPersistenceMapper.toEntity(offer)))))
                .subscribeOn(jpaScheduler);
    }

    @Override
    public Mono<Offer> findById(Long id) {
        return Mono.fromCallable(() -> readTransaction.execute(status ->
                        repository.findByIdWithServices(id).map(OfferPersistenceMapper::toDomain).orElse(null)))
                .subscribeOn(jpaScheduler);
    }

    @Override
    public Flux<Offer> findAll() {
        return Mono.fromCallable(() -> readTransaction.execute(status ->
                        repository.findAllWithServices().stream().map(OfferPersistenceMapper::toDomain).toList()))
                .flatMapMany(Flux::<Offer>fromIterable)
                .subscribeOn(jpaScheduler);
    }
}
