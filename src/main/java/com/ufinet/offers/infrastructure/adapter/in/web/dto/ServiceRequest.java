package com.ufinet.offers.infrastructure.adapter.in.web.dto;

import com.ufinet.offers.domain.model.ServiceType;
import com.ufinet.offers.infrastructure.adapter.in.web.validation.ValidContractTerm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Servicio a incluir en la oferta")
public record ServiceRequest(
        @Schema(description = "Tipo de servicio", example = "INTERNET")
        @NotNull(message = "El tipo es obligatorio (INTERNET o CAPACIDAD)")
        ServiceType tipo,

        @Schema(description = "Ancho de banda en Mbps (10 a 10.000)", example = "100")
        @NotNull(message = "Los Mbps son obligatorios")
        @Min(value = 10, message = "Los Mbps deben estar entre 10 y 10000")
        @Max(value = 10_000, message = "Los Mbps deben estar entre 10 y 10000")
        Integer mbps,

        @Schema(description = "Plazo del contrato en meses", allowableValues = {"12", "24", "36"}, example = "36")
        @NotNull(message = "El plazo es obligatorio")
        @ValidContractTerm
        Integer plazoMeses,

        @Schema(description = "Indica si el servicio es técnicamente viable", example = "true")
        @NotNull(message = "El indicador viable es obligatorio")
        Boolean viable
) {
}
