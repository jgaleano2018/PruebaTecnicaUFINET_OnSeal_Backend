package com.ufinet.offers.infrastructure.adapter.in.web.handler;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Manejador global de errores (unitario)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ProblemDetailFactory(Clock.systemUTC()));
    private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/offers"));

    @Test
    @DisplayName("Construye la ruta del campo a partir del path de Jackson")
    void buildsFieldPath() {
        InvalidFormatException ex = new InvalidFormatException((JsonParser) null, "bad", "VOZ", Object.class);
        ex.prependPath(new JsonMappingException.Reference(null, "tipo"));
        ex.prependPath(new JsonMappingException.Reference(null, 0));
        ex.prependPath(new JsonMappingException.Reference(null, "servicios"));

        ApiFieldError error = GlobalExceptionHandler.toFieldError(ex);

        assertThat(error).isEqualTo(new ApiFieldError("servicios[0].tipo", "El valor 'VOZ' no es válido para este campo"));
    }

    @Test
    @DisplayName("Tipos de dato incorrectos y errores sin ruta")
    void mismatchedAndGenericErrors() {
        MismatchedInputException mismatched = MismatchedInputException.from((JsonParser) null, Integer.class, "bad");
        mismatched.prependPath(new JsonMappingException.Reference(null, "mbps"));
        assertThat(GlobalExceptionHandler.toFieldError(mismatched))
                .isEqualTo(new ApiFieldError("mbps", "El tipo de dato no es válido para este campo"));

        JsonMappingException generic = new JsonMappingException((java.io.Closeable) null, "bad");
        assertThat(GlobalExceptionHandler.toFieldError(generic))
                .isEqualTo(new ApiFieldError("body", "El valor no se pudo interpretar"));
    }

    @Test
    @DisplayName("ServerWebInputException sin causa conocida -> 400 MALFORMED_REQUEST sin errores de campo")
    void inputExceptionWithoutCause() {
        ResponseEntity<ProblemDetail> response =
                handler.handleInputException(new ServerWebInputException("No request body"), exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getProperties()).containsEntry("code", "MALFORMED_REQUEST");
        assertThat(response.getBody().getDetail()).isEqualTo(GlobalExceptionHandler.MALFORMED_DETAIL);
    }

    @Test
    @DisplayName("ResponseStatusException conserva su código y razón")
    void responseStatusException() {
        ResponseEntity<ProblemDetail> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "Método no soportado"), exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Method Not Allowed");
        assertThat(response.getBody().getDetail()).isEqualTo("Método no soportado");
    }

    @Test
    @DisplayName("ResponseStatusException sin razón usa el título")
    void responseStatusExceptionWithoutReason() {
        ResponseEntity<ProblemDetail> response =
                handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND), exchange);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Not Found");
    }
}
