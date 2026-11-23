package com.ufinet.offers.infrastructure.adapter.in.web;

import com.ufinet.offers.application.port.in.CreateOfferUseCase;
import com.ufinet.offers.application.port.in.GetOfferUseCase;
import com.ufinet.offers.application.port.in.ListOffersUseCase;
import com.ufinet.offers.application.port.in.PresentOfferUseCase;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.CreateOfferRequest;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferSummaryResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.mapper.OfferWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Adaptador de entrada HTTP (WebFlux) para el recurso ofertas.
 */
@RestController
@RequestMapping(value = "/api/offers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Ofertas", description = "Creación, consulta y presentación de ofertas")
public class OfferController {

    private final CreateOfferUseCase createOfferUseCase;
    private final ListOffersUseCase listOffersUseCase;
    private final GetOfferUseCase getOfferUseCase;
    private final PresentOfferUseCase presentOfferUseCase;

    public OfferController(CreateOfferUseCase createOfferUseCase,
                           ListOffersUseCase listOffersUseCase,
                           GetOfferUseCase getOfferUseCase,
                           PresentOfferUseCase presentOfferUseCase) {
        this.createOfferUseCase = createOfferUseCase;
        this.listOffersUseCase = listOffersUseCase;
        this.getOfferUseCase = getOfferUseCase;
        this.presentOfferUseCase = presentOfferUseCase;
    }

    @PostMapping
    @Operation(summary = "Crea una oferta en BORRADOR y calcula el MRC de cada servicio")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Oferta creada"),
            @ApiResponse(responseCode = "400", description = "Falla una validación",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Mono<ResponseEntity<OfferResponse>> create(@Valid @RequestBody CreateOfferRequest request) {
        return createOfferUseCase.create(OfferWebMapper.toCommand(request))
                .map(OfferWebMapper::toResponse)
                .map(body -> ResponseEntity.created(URI.create("/api/offers/" + body.id())).body(body));
    }

    @GetMapping
    @Operation(summary = "Lista las ofertas con id, cliente, estado y mrcTotal")
    @ApiResponse(responseCode = "200", description = "Listado de ofertas")
    public Flux<OfferSummaryResponse> list() {
        return listOffersUseCase.listAll().map(OfferWebMapper::toSummary);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Devuelve la oferta con sus servicios y mrcTotal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oferta encontrada"),
            @ApiResponse(responseCode = "404", description = "La oferta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Mono<OfferResponse> getById(@Parameter(description = "Id de la oferta") @PathVariable("id") Long id) {
        return getOfferUseCase.getById(id).map(OfferWebMapper::toResponse);
    }

    @PostMapping("/{id}/present")
    @Operation(summary = "Aplica la regla de presentación (PRESENTADA o NO_VIABLE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oferta presentada"),
            @ApiResponse(responseCode = "404", description = "La oferta no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "La oferta no está en BORRADOR",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Mono<OfferResponse> present(@Parameter(description = "Id de la oferta") @PathVariable("id") Long id) {
        return presentOfferUseCase.present(id).map(OfferWebMapper::toResponse);
    }
}
