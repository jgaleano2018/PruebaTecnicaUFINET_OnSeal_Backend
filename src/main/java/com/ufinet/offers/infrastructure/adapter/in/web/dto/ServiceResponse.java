package com.ufinet.offers.infrastructure.adapter.in.web.dto;

import com.ufinet.offers.domain.model.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio de una oferta con su MRC calculado")
public record ServiceResponse(
        Long id,
        ServiceType tipo,
        Integer mbps,
        Integer plazoMeses,
        Boolean viable,
        @Schema(description = "Cargo mensual recurrente en USD", example = "225.00") BigDecimal mrc
) {
}
