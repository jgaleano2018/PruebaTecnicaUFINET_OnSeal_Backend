package com.ufinet.offers.infrastructure.adapter.in.web.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.ufinet.offers.domain.exception.DomainValidationException;
import com.ufinet.offers.domain.exception.InvalidOfferStateException;
import com.ufinet.offers.domain.exception.OfferNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones a respuestas HTTP con formato ProblemDetail homogéneo.
 * <ul>
 *     <li>400: validaciones de entrada y del dominio, JSON mal formado.</li>
 *     <li>404: la oferta no existe.</li>
 *     <li>409: la oferta no está en BORRADOR o fue modificada concurrentemente.</li>
 *     <li>500: error no controlado (sin exponer detalles internos).</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String VALIDATION_TITLE = "Solicitud inválida";
    static final String VALIDATION_DETAIL = "La solicitud contiene errores de validación";
    static final String MALFORMED_DETAIL =
            "El cuerpo de la petición no es válido: revise el formato JSON y los valores enviados";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailFactory problemFactory;

    public GlobalExceptionHandler(ProblemDetailFactory problemFactory) {
        this.problemFactory = problemFactory;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        List<ApiFieldError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errors.add(new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : ex.getGlobalErrors()) {
            errors.add(new ApiFieldError(globalError.getObjectName(), globalError.getDefaultMessage()));
        }
        return problemFactory.build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, VALIDATION_TITLE,
                VALIDATION_DETAIL, exchange, errors);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException ex,
                                                                ServerWebExchange exchange) {
        List<ApiFieldError> errors = ex.getErrors().stream()
                .map(error -> new ApiFieldError(error.field(), error.message()))
                .toList();
        return problemFactory.build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, VALIDATION_TITLE,
                VALIDATION_DETAIL, exchange, errors);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetail> handleInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        if (ex.getCause() instanceof TypeMismatchException typeMismatch) {
            String parameter = Optional.ofNullable(typeMismatch.getPropertyName())
                    .or(() -> Optional.ofNullable(ex.getMethodParameter()).map(p -> p.getParameterName()))
                    .orElse("parámetro");
            String detail = "El valor '%s' no es válido para '%s'".formatted(typeMismatch.getValue(), parameter);
            return problemFactory.build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, VALIDATION_TITLE,
                    detail, exchange, List.of(new ApiFieldError(parameter, detail)));
        }
        List<ApiFieldError> errors = findJsonMappingError(ex)
                .map(GlobalExceptionHandler::toFieldError)
                .map(List::of)
                .orElse(List.of());
        return problemFactory.build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, VALIDATION_TITLE,
                MALFORMED_DETAIL, exchange, errors);
    }

    @ExceptionHandler(OfferNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(OfferNotFoundException ex, ServerWebExchange exchange) {
        return problemFactory.build(HttpStatus.NOT_FOUND, ErrorCode.OFFER_NOT_FOUND, "Oferta no encontrada",
                ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(InvalidOfferStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidState(InvalidOfferStateException ex,
                                                            ServerWebExchange exchange) {
        return problemFactory.build(HttpStatus.CONFLICT, ErrorCode.INVALID_OFFER_STATE,
                "Estado de la oferta no válido", ex.getMessage(), exchange, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex,
                                                              ServerWebExchange exchange) {
        return problemFactory.build(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION,
                "Conflicto de concurrencia",
                "La oferta fue modificada por otra operación; consulte su estado actual e intente de nuevo",
                exchange, List.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex,
                                                              ServerWebExchange exchange) {
        String title = Optional.ofNullable(HttpStatus.resolve(ex.getStatusCode().value()))
                .map(HttpStatus::getReasonPhrase)
                .orElse("Error HTTP");
        String detail = Optional.ofNullable(ex.getReason()).orElse(title);
        return problemFactory.build(ex.getStatusCode(), ErrorCode.HTTP_ERROR, title, detail, exchange, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Error no controlado procesando {}", exchange.getRequest().getPath(), ex);
        return problemFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Error interno", "Ocurrió un error inesperado; intente de nuevo más tarde", exchange, List.of());
    }

    private static Optional<JsonMappingException> findJsonMappingError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof JsonMappingException mappingException) {
                return Optional.of(mappingException);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    static ApiFieldError toFieldError(JsonMappingException ex) {
        String field = ex.getPath().stream()
                .map(reference -> reference.getFieldName() != null
                        ? "." + reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining())
                .replaceFirst("^\\.", "");
        String message;
        if (ex instanceof InvalidFormatException invalidFormat) {
            message = "El valor '%s' no es válido para este campo".formatted(invalidFormat.getValue());
        } else if (ex instanceof MismatchedInputException) {
            message = "El tipo de dato no es válido para este campo";
        } else {
            message = "El valor no se pudo interpretar";
        }
        return new ApiFieldError(field.isEmpty() ? "body" : field, message);
    }
}
