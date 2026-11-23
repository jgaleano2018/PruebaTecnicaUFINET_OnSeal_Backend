package com.ufinet.offers.infrastructure.adapter.in.web.dto;

import com.ufinet.offers.domain.model.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumen de una oferta para el listado")
public record OfferSummaryResponse(
        Long id,
        String cliente,
        OfferStatus estado,
        @Schema(example = "343.75") BigDecimal mrcTotal
) {
}
