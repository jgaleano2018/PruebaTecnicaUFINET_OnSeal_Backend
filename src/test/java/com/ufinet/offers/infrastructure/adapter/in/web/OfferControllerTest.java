package com.ufinet.offers.infrastructure.adapter.in.web;

import com.ufinet.offers.OfferFixtures;
import com.ufinet.offers.application.port.in.CreateOfferCommand;
import com.ufinet.offers.application.port.in.CreateOfferUseCase;
import com.ufinet.offers.application.port.in.GetOfferUseCase;
import com.ufinet.offers.application.port.in.ListOffersUseCase;
import com.ufinet.offers.application.port.in.PresentOfferUseCase;
import com.ufinet.offers.domain.exception.DomainValidationException;
import com.ufinet.offers.domain.exception.InvalidOfferStateException;
import com.ufinet.offers.domain.exception.OfferNotFoundException;
import com.ufinet.offers.domain.exception.ValidationError;
import com.ufinet.offers.domain.model.OfferStatus;
import com.ufinet.offers.infrastructure.adapter.in.web.handler.ProblemDetailFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentCaptor.forClass;

@WebFluxTest(controllers = OfferController.class)
@DisplayName("API REST de ofertas (capa web)")
class OfferControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ProblemDetailFactory problemDetailFactory() {
            return new ProblemDetailFactory(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        }
    }

    @Autowired
    private WebTestClient client;

    @MockitoBean
    private CreateOfferUseCase createOfferUseCase;
    @MockitoBean
    private ListOffersUseCase listOffersUseCase;
    @MockitoBean
    private GetOfferUseCase getOfferUseCase;
    @MockitoBean
    private PresentOfferUseCase presentOfferUseCase;

    @Nested
    @DisplayName("POST /api/offers")
    class Create {

        @Test
        @DisplayName("201 con la oferta, su MRC y la cabecera Location")
        void createsOffer() {
            when(createOfferUseCase.create(any())).thenReturn(Mono.just(OfferFixtures.draftOffer(10L)));

            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(OfferFixtures.validCreateRequestJson())
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().location("/api/offers/10")
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(10)
                    .jsonPath("$.cliente").isEqualTo(OfferFixtures.CLIENT)
                    .jsonPath("$.estado").isEqualTo("BORRADOR")
                    .jsonPath("$.servicios.length()").isEqualTo(2)
                    .jsonPath("$.servicios[0].tipo").isEqualTo("INTERNET")
                    .jsonPath("$.servicios[0].plazoMeses").isEqualTo(36)
                    .jsonPath("$.servicios[0].mrc").isEqualTo(225.00)
                    .jsonPath("$.servicios[1].mrc").isEqualTo(118.75)
                    .jsonPath("$.mrcTotal").isEqualTo(343.75);

            var captor = forClass(CreateOfferCommand.class);
            verify(createOfferUseCase).create(captor.capture());
            assertThat(captor.getValue().client()).isEqualTo(OfferFixtures.CLIENT);
            assertThat(captor.getValue().services()).hasSize(2);
        }

        @Test
        @DisplayName("400 con los errores de validación por campo")
        void validationErrors() {
            String body = """
                    {
                      "cliente": "",
                      "servicios": [
                        { "tipo": "INTERNET", "mbps": 5, "plazoMeses": 18, "viable": null }
                      ]
                    }
                    """;

            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(400)
                    .jsonPath("$.title").isEqualTo("Solicitud inválida")
                    .jsonPath("$.detail").isEqualTo("La solicitud contiene errores de validación")
                    .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                    .jsonPath("$.instance").isEqualTo("/api/offers")
                    .jsonPath("$.timestamp").isEqualTo("2026-01-01T00:00:00Z")
                    .jsonPath("$.errors.length()").isEqualTo(4)
                    .jsonPath("$.errors[?(@.field == 'cliente')].message").isEqualTo("El cliente es obligatorio")
                    .jsonPath("$.errors[?(@.field == 'servicios[0].mbps')].message")
                    .isEqualTo("Los Mbps deben estar entre 10 y 10000")
                    .jsonPath("$.errors[?(@.field == 'servicios[0].plazoMeses')].message")
                    .isEqualTo("El plazo debe ser 12, 24 o 36 meses")
                    .jsonPath("$.errors[?(@.field == 'servicios[0].viable')].message")
                    .isEqualTo("El indicador viable es obligatorio");

            verifyNoInteractions(createOfferUseCase);
        }

        @Test
        @DisplayName("400 si hay más de 5 servicios o ninguno")
        void servicesCount() {
            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"cliente\":\"Cliente\",\"servicios\":[]}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.errors[0].field").isEqualTo("servicios")
                    .jsonPath("$.errors[0].message").isEqualTo("La oferta debe tener entre 1 y 5 servicios");
        }

        @Test
        @DisplayName("400 si el cliente supera 100 caracteres")
        void clientTooLong() {
            String body = "{\"cliente\":\"" + "x".repeat(101) + "\",\"servicios\":[{\"tipo\":\"INTERNET\","
                    + "\"mbps\":100,\"plazoMeses\":12,\"viable\":true}]}";

            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.errors[0].field").isEqualTo("cliente")
                    .jsonPath("$.errors[0].message").isEqualTo("El cliente no puede superar 100 caracteres");
        }

        @Test
        @DisplayName("400 si el tipo de servicio no existe")
        void invalidEnum() {
            String body = "{\"cliente\":\"Cliente\",\"servicios\":[{\"tipo\":\"VOZ\",\"mbps\":100,"
                    + "\"plazoMeses\":12,\"viable\":true}]}";

            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST")
                    .jsonPath("$.errors[0].field").isEqualTo("servicios[0].tipo")
                    .jsonPath("$.errors[0].message").isEqualTo("El valor 'VOZ' no es válido para este campo");
        }

        @Test
        @DisplayName("400 si el JSON está mal formado")
        void malformedJson() {
            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{ \"cliente\": ")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST")
                    .jsonPath("$.status").isEqualTo(400);
        }

        @Test
        @DisplayName("400 cuando el dominio rechaza la oferta")
        void domainValidation() {
            when(createOfferUseCase.create(any())).thenReturn(Mono.error(new DomainValidationException(
                    List.of(new ValidationError("servicios[0].plazoMeses", "El plazo debe ser 12, 24 o 36 meses")))));

            client.post().uri("/api/offers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(OfferFixtures.validCreateRequestJson())
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
                    .jsonPath("$.errors[0].field").isEqualTo("servicios[0].plazoMeses");
        }

        @Test
        @DisplayName("415 si el contenido no es JSON (mismo formato ProblemDetail)")
        void unsupportedMediaType() {
            client.post().uri("/api/offers")
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue("hola")
                    .exchange()
                    .expectStatus().isEqualTo(415)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("HTTP_ERROR")
                    .jsonPath("$.status").isEqualTo(415);
        }
    }

    @Nested
    @DisplayName("GET /api/offers")
    class ListOffers {

        @Test
        @DisplayName("200 con id, cliente, estado y mrcTotal")
        void listsOffers() {
            when(listOffersUseCase.listAll()).thenReturn(Flux.just(
                    OfferFixtures.draftOffer(1L), OfferFixtures.offer(2L, OfferStatus.NO_VIABLE, false)));

            client.get().uri("/api/offers")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[0].id").isEqualTo(1)
                    .jsonPath("$[0].cliente").isEqualTo(OfferFixtures.CLIENT)
                    .jsonPath("$[0].estado").isEqualTo("BORRADOR")
                    .jsonPath("$[0].mrcTotal").isEqualTo(343.75)
                    .jsonPath("$[0].servicios").doesNotExist()
                    .jsonPath("$[1].estado").isEqualTo("NO_VIABLE");
        }

        @Test
        @DisplayName("200 con lista vacía")
        void emptyList() {
            when(listOffersUseCase.listAll()).thenReturn(Flux.empty());

            client.get().uri("/api/offers").exchange()
                    .expectStatus().isOk()
                    .expectBody().json("[]");
        }
    }

    @Nested
    @DisplayName("GET /api/offers/{id}")
    class GetById {

        @Test
        @DisplayName("200 con servicios y mrcTotal")
        void returnsOffer() {
            when(getOfferUseCase.getById(1L)).thenReturn(Mono.just(OfferFixtures.draftOffer(1L)));

            client.get().uri("/api/offers/1").exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(1)
                    .jsonPath("$.version").isEqualTo(0)
                    .jsonPath("$.servicios[1].tipo").isEqualTo("CAPACIDAD")
                    .jsonPath("$.mrcTotal").isEqualTo(343.75);
        }

        @Test
        @DisplayName("404 si no existe")
        void notFound() {
            when(getOfferUseCase.getById(99L)).thenReturn(Mono.error(new OfferNotFoundException(99L)));

            client.get().uri("/api/offers/99").exchange()
                    .expectStatus().isNotFound()
                    .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.title").isEqualTo("Oferta no encontrada")
                    .jsonPath("$.detail").isEqualTo("No existe una oferta con id 99")
                    .jsonPath("$.code").isEqualTo("OFFER_NOT_FOUND")
                    .jsonPath("$.instance").isEqualTo("/api/offers/99")
                    .jsonPath("$.errors.length()").isEqualTo(0);
        }

        @Test
        @DisplayName("400 si el id no es numérico")
        void invalidId() {
            client.get().uri("/api/offers/abc").exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("MALFORMED_REQUEST")
                    .jsonPath("$.detail").isEqualTo("El valor 'abc' no es válido para 'id'");

            verifyNoInteractions(getOfferUseCase);
        }

        @Test
        @DisplayName("500 sin exponer detalles internos ante un error inesperado")
        void unexpectedError() {
            when(getOfferUseCase.getById(1L)).thenReturn(Mono.error(new IllegalStateException("boom")));

            client.get().uri("/api/offers/1").exchange()
                    .expectStatus().is5xxServerError()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                    .jsonPath("$.detail").isEqualTo("Ocurrió un error inesperado; intente de nuevo más tarde");
        }
    }

    @Nested
    @DisplayName("POST /api/offers/{id}/present")
    class Present {

        @Test
        @DisplayName("200 con la oferta PRESENTADA")
        void presents() {
            when(presentOfferUseCase.present(1L))
                    .thenReturn(Mono.just(OfferFixtures.offer(1L, OfferStatus.PRESENTADA, true)));

            client.post().uri("/api/offers/1/present").exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.estado").isEqualTo("PRESENTADA");
        }

        @Test
        @DisplayName("404 si la oferta no existe")
        void notFound() {
            when(presentOfferUseCase.present(99L)).thenReturn(Mono.error(new OfferNotFoundException(99L)));

            client.post().uri("/api/offers/99/present").exchange()
                    .expectStatus().isNotFound()
                    .expectBody().jsonPath("$.code").isEqualTo("OFFER_NOT_FOUND");
        }

        @Test
        @DisplayName("Test mínimo 4: presentar una oferta ya presentada -> 409")
        void conflictWhenNotDraft() {
            when(presentOfferUseCase.present(1L))
                    .thenReturn(Mono.error(new InvalidOfferStateException(1L, OfferStatus.PRESENTADA)));

            client.post().uri("/api/offers/1/present").exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(409)
                    .jsonPath("$.code").isEqualTo("INVALID_OFFER_STATE")
                    .jsonPath("$.detail").isEqualTo("La oferta 1 no se puede presentar porque su estado es PRESENTADA; "
                            + "solo se pueden presentar ofertas en BORRADOR");
        }

        @Test
        @DisplayName("409 ante una modificación concurrente (bloqueo optimista)")
        void conflictOnOptimisticLock() {
            when(presentOfferUseCase.present(1L))
                    .thenReturn(Mono.error(new OptimisticLockingFailureException("stale")));

            client.post().uri("/api/offers/1/present").exchange()
                    .expectStatus().isEqualTo(409)
                    .expectBody().jsonPath("$.code").isEqualTo("CONCURRENT_MODIFICATION");

            verify(createOfferUseCase, never()).create(any());
        }
    }
}
