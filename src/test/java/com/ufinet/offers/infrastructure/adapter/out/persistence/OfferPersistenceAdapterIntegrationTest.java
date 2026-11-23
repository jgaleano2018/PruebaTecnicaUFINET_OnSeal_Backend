package com.ufinet.offers.infrastructure.adapter.out.persistence;

import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.domain.model.ServiceSpec;
import com.ufinet.offers.domain.model.ServiceType;
import com.ufinet.offers.domain.service.OfferFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Adaptador de persistencia JPA/H2 (integración)")
class OfferPersistenceAdapterIntegrationTest {

    @Autowired
    private OfferPersistenceAdapter adapter;

    @Autowired
    private OfferFactory offerFactory;

    private Offer newDraft(boolean secondViable) {
        return offerFactory.createDraft("Cliente Persistencia", List.of(
                new ServiceSpec(ServiceType.INTERNET, 100, 36, true),
                new ServiceSpec(ServiceType.CAPACIDAD, 50, 24, secondViable)));
    }

    @Test
    @DisplayName("Guarda la oferta con sus servicios, id y versión inicial")
    void savesNewOffer() {
        StepVerifier.create(adapter.save(newDraft(true)))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getVersion()).isZero();
                    assertThat(saved.getStatus()).isEqualTo(OfferStatus.BORRADOR);
                    assertThat(saved.getServices()).hasSize(2)
                            .allSatisfy(service -> assertThat(service.getId()).isNotNull());
                    assertThat(saved.getMrcTotal()).isEqualByComparingTo("343.75");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Recupera por id y en el listado, con el MRC persistido")
    void findsOffers() {
        Offer saved = adapter.save(newDraft(false)).block();
        assertThat(saved).isNotNull();

        StepVerifier.create(adapter.findById(saved.getId()))
                .assertNext(found -> {
                    assertThat(found.getClient()).isEqualTo("Cliente Persistencia");
                    assertThat(found.getServices()).extracting(s -> s.getMrc().toPlainString())
                            .containsExactly("225.00", "118.75");
                    assertThat(found.allServicesViable()).isFalse();
                })
                .verifyComplete();

        StepVerifier.create(adapter.findAll().filter(offer -> offer.getId().equals(saved.getId())))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("findById de un id inexistente es vacío")
    void findByIdEmpty() {
        StepVerifier.create(adapter.findById(Long.MAX_VALUE)).verifyComplete();
    }

    @Test
    @DisplayName("Actualiza el estado e incrementa la versión")
    void updatesStatusAndVersion() {
        Offer saved = adapter.save(newDraft(true)).block();
        assertThat(saved).isNotNull();

        StepVerifier.create(adapter.save(saved.withStatus(OfferStatus.PRESENTADA)))
                .assertNext(updated -> {
                    assertThat(updated.getStatus()).isEqualTo(OfferStatus.PRESENTADA);
                    assertThat(updated.getVersion()).isEqualTo(1L);
                    assertThat(updated.getServices()).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Bloqueo optimista: guardar una versión obsoleta falla")
    void optimisticLocking() {
        Offer saved = adapter.save(newDraft(true)).block();
        assertThat(saved).isNotNull();
        adapter.save(saved.withStatus(OfferStatus.PRESENTADA)).block();

        StepVerifier.create(adapter.save(saved.withStatus(OfferStatus.NO_VIABLE)))
                .expectError(OptimisticLockingFailureException.class)
                .verify();

        StepVerifier.create(adapter.findById(saved.getId()))
                .assertNext(current -> assertThat(current.getStatus()).isEqualTo(OfferStatus.PRESENTADA))
                .verifyComplete();
    }
}
