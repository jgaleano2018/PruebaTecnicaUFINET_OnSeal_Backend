package com.ufinet.offers.infrastructure.adapter.in.web.handler;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Construye todas las respuestas de error con el mismo formato (RFC 9457):
 * <pre>
 * { type, title, status, detail, instance, code, timestamp, errors[] }
 * </pre>
 */
public class ProblemDetailFactory {

    static final String TYPE_PREFIX = "urn:ufinet:problem:";

    private final Clock clock;

    public ProblemDetailFactory(Clock clock) {
        this.clock = clock;
    }

    public ResponseEntity<ProblemDetail> build(HttpStatusCode status, ErrorCode code, String title, String detail,
                                               ServerWebExchange exchange, List<ApiFieldError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_PREFIX + code.name().toLowerCase().replace('_', '-')));
        if (exchange != null) {
            problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
        }
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now(clock).toString());
        problem.setProperty("errors", errors == null ? List.of() : List.copyOf(errors));
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
