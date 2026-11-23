package com.ufinet.offers.infrastructure.adapter.in.web.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fábrica de ProblemDetail")
class ProblemDetailFactoryTest {

    private final ProblemDetailFactory factory =
            new ProblemDetailFactory(Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("Construye el formato homogéneo de error")
    void buildsProblem() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/offers/1"));

        ResponseEntity<ProblemDetail> response = factory.build(HttpStatus.NOT_FOUND, ErrorCode.OFFER_NOT_FOUND,
                "Oferta no encontrada", "No existe", exchange, List.of(new ApiFieldError("id", "x")));

        ProblemDetail problem = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(problem).isNotNull();
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Oferta no encontrada");
        assertThat(problem.getDetail()).isEqualTo("No existe");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:ufinet:problem:offer-not-found"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/api/offers/1"));
        assertThat(problem.getProperties())
                .containsEntry("code", "OFFER_NOT_FOUND")
                .containsEntry("timestamp", "2026-05-01T10:00:00Z")
                .containsEntry("errors", List.of(new ApiFieldError("id", "x")));
    }

    @Test
    @DisplayName("Sin exchange ni errores usa valores por defecto")
    void buildsProblemWithoutExchange() {
        ProblemDetail problem = factory.build(HttpStatus.CONFLICT, ErrorCode.INVALID_OFFER_STATE, "t", "d",
                null, null).getBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getInstance()).isNull();
        assertThat(problem.getProperties()).containsEntry("errors", List.of());
    }
}
