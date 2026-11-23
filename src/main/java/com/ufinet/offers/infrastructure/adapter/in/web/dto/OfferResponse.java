package com.ufinet.offers.infrastructure.adapter.in.web.dto;

import com.ufinet.offers.domain.model.OfferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Oferta con sus servicios y MRC total")
public record OfferResponse(
        Long id,
        String cliente,
        OfferStatus estado,
        Long version,
        List<ServiceResponse> servicios,
        @Schema(description = "Suma del MRC de los servicios en USD", example = "343.75") BigDecimal mrcTotal
) {
}
