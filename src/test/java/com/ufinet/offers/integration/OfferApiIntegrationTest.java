package com.ufinet.offers.integration;

import com.ufinet.offers.OfferFixtures;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("API de ofertas de extremo a extremo (WebFlux + JPA + H2)")
class OfferApiIntegrationTest {

    @Autowired
    private WebTestClient client;

    private OfferResponse create(String json) {
        OfferResponse created = client.post().uri("/api/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OfferResponse.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        return created;
    }

    @Test
    @DisplayName("Crear, consultar, listar, presentar (PRESENTADA) y rechazar la segunda presentación (409)")
    void fullFlowPresented() {
        OfferResponse created = create(OfferFixtures.validCreateRequestJson());
        assertThat(created.estado().name()).isEqualTo("BORRADOR");
        assertThat(created.mrcTotal()).isEqualByComparingTo("343.75");

        client.get().uri("/api/offers/{id}", created.id()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.servicios[0].mrc").isEqualTo(225.00)
                .jsonPath("$.servicios[1].mrc").isEqualTo(118.75)
                .jsonPath("$.mrcTotal").isEqualTo(343.75);

        client.get().uri("/api/offers").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id == " + created.id() + ")].estado").isEqualTo("BORRADOR");

        client.post().uri("/api/offers/{id}/present", created.id()).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("PRESENTADA")
                .jsonPath("$.version").isEqualTo(1);

        client.post().uri("/api/offers/{id}/present", created.id()).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_OFFER_STATE");

        client.get().uri("/api/offers/{id}", created.id()).exchange()
                .expectBody().jsonPath("$.estado").isEqualTo("PRESENTADA");
    }

    @Test
    @DisplayName("Un servicio no viable deja la oferta en NO_VIABLE")
    void flowNotViable() {
        OfferResponse created = create("""
                {"cliente":"No Viable S.A.","servicios":[
                  {"tipo":"INTERNET","mbps":100,"plazoMeses":12,"viable":true},
                  {"tipo":"CAPACIDAD","mbps":10,"plazoMeses":36,"viable":false}]}
                """);

        client.post().uri("/api/offers/{id}/present", created.id()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.estado").isEqualTo("NO_VIABLE");
    }

    @Test
    @DisplayName("404 al consultar o presentar una oferta inexistente")
    void notFound() {
        client.get().uri("/api/offers/999999").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("No existe una oferta con id 999999");
        client.post().uri("/api/offers/999999/present").exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("400 con formato ProblemDetail cuando falla una validación")
    void validation() {
        client.post().uri("/api/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"cliente\":\"X\",\"servicios\":[{\"tipo\":\"INTERNET\",\"mbps\":20000,"
                        + "\"plazoMeses\":12,\"viable\":true}]}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.errors[0].field").isEqualTo("servicios[0].mbps");
    }

    @Test
    @DisplayName("CORS permitido solo para http://localhost:5173")
    void cors() {
        client.options().uri("/api/offers")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");

        client.options().uri("/api/offers")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("La documentación OpenAPI está publicada")
    void openApiDocs() {
        client.get().uri("/v3/api-docs").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['/api/offers']").exists()
                .jsonPath("$.paths['/api/offers/{id}/present']").exists();
    }
}
