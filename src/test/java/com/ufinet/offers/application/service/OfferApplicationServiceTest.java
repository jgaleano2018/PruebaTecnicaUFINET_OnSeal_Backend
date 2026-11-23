package com.ufinet.offers.application.service;

import com.ufinet.offers.OfferFixtures;
import com.ufinet.offers.application.port.in.CreateOfferCommand;
import com.ufinet.offers.application.port.out.OfferRepositoryPort;
import com.ufinet.offers.domain.exception.DomainValidationException;
import com.ufinet.offers.domain.exception.InvalidOfferStateException;
import com.ufinet.offers.domain.exception.OfferNotFoundException;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.domain.model.ServiceSpec;
import com.ufinet.offers.domain.model.ServiceType;
import com.ufinet.offers.domain.service.OfferFactory;
import com.ufinet.offers.domain.service.PresentationPolicy;
import com.ufinet.offers.domain.service.PricingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de aplicación de ofertas")
class OfferApplicationServiceTest {

    @Mock
    private OfferRepositoryPort repository;

    private OfferApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OfferApplicationService(repository, new OfferFactory(new PricingPolicy()), new PresentationPolicy());
    }

    @Test
    @DisplayName("Crear: construye la oferta en BORRADOR con MRC y la persiste")
    void createPersistsDraft() {
        when(repository.save(any(Offer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0, Offer.class)));
        CreateOfferCommand command = new CreateOfferCommand(OfferFixtures.CLIENT, List.of(
                new ServiceSpec(ServiceType.INTERNET, 100, 36, true),
                new ServiceSpec(ServiceType.CAPACIDAD, 50, 24, true)));

        StepVerifier.create(service.create(command))
                .assertNext(offer -> {
                    assertThat(offer.getStatus()).isEqualTo(OfferStatus.BORRADOR);
                    assertThat(offer.getMrcTotal()).isEqualByComparingTo("343.75");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Crear: si la validación falla no persiste nada")
    void createWithInvalidDataDoesNotPersist() {
        CreateOfferCommand command = new CreateOfferCommand("", List.of());

        StepVerifier.create(service.create(command))
                .expectError(DomainValidationException.class)
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Listar: delega en el repositorio")
    void listAllDelegatesToRepository() {
        when(repository.findAll()).thenReturn(Flux.just(OfferFixtures.draftOffer(1L), OfferFixtures.draftOffer(2L)));

        StepVerifier.create(service.listAll())
                .expectNextMatches(offer -> offer.getId() == 1L)
                .expectNextMatches(offer -> offer.getId() == 2L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Obtener: devuelve la oferta existente")
    void getByIdReturnsOffer() {
        when(repository.findById(1L)).thenReturn(Mono.just(OfferFixtures.draftOffer(1L)));

        StepVerifier.create(service.getById(1L))
                .expectNextMatches(offer -> offer.getId() == 1L)
                .verifyComplete();
    }

    @Test
    @DisplayName("Obtener: emite OfferNotFoundException si no existe")
    void getByIdNotFound() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(99L))
                .expectErrorSatisfies(ex -> assertThat(ex)
                        .isInstanceOf(OfferNotFoundException.class)
                        .hasMessage("No existe una oferta con id 99"))
                .verify();
    }

    @Test
    @DisplayName("Presentar: todos viables -> guarda la oferta PRESENTADA")
    void presentAllViable() {
        when(repository.findById(1L)).thenReturn(Mono.just(OfferFixtures.offer(1L, OfferStatus.BORRADOR, true)));
        when(repository.save(any(Offer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0, Offer.class)));

        StepVerifier.create(service.present(1L))
                .assertNext(offer -> assertThat(offer.getStatus()).isEqualTo(OfferStatus.PRESENTADA))
                .verifyComplete();

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OfferStatus.PRESENTADA);
    }

    @Test
    @DisplayName("Presentar: un servicio no viable -> NO_VIABLE")
    void presentWithNonViable() {
        when(repository.findById(1L)).thenReturn(Mono.just(OfferFixtures.offer(1L, OfferStatus.BORRADOR, false)));
        when(repository.save(any(Offer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0, Offer.class)));

        StepVerifier.create(service.present(1L))
                .assertNext(offer -> assertThat(offer.getStatus()).isEqualTo(OfferStatus.NO_VIABLE))
                .verifyComplete();
    }

    @Test
    @DisplayName("Presentar: una oferta ya presentada falla y no se guarda nada")
    void presentAlreadyPresented() {
        when(repository.findById(1L)).thenReturn(Mono.just(OfferFixtures.offer(1L, OfferStatus.PRESENTADA, true)));

        StepVerifier.create(service.present(1L))
                .expectError(InvalidOfferStateException.class)
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Presentar: oferta inexistente -> OfferNotFoundException")
    void presentNotFound() {
        when(repository.findById(5L)).thenReturn(Mono.empty());

        StepVerifier.create(service.present(5L))
                .expectError(OfferNotFoundException.class)
                .verify();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Las dependencias son obligatorias")
    void requiresDependencies() {
        OfferFactory factory = new OfferFactory(new PricingPolicy());
        PresentationPolicy policy = new PresentationPolicy();
        assertThatNullPointerException().isThrownBy(() -> new OfferApplicationService(null, factory, policy));
        assertThatNullPointerException().isThrownBy(() -> new OfferApplicationService(repository, null, policy));
        assertThatNullPointerException().isThrownBy(() -> new OfferApplicationService(repository, factory, null));
    }
}
