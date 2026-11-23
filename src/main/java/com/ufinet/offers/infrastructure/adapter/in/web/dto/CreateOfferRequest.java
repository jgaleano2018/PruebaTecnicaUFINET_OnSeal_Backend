package com.ufinet.offers.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Datos para crear una oferta")
public record CreateOfferRequest(
        @Schema(description = "Nombre del cliente", example = "Cliente Demo S.A.", maxLength = 100)
        @NotBlank(message = "El cliente es obligatorio")
        @Size(max = 100, message = "El cliente no puede superar 100 caracteres")
        String cliente,

        @ArraySchema(arraySchema = @Schema(description = "Servicios de la oferta (1 a 5)"), minItems = 1, maxItems = 5)
        @NotNull(message = "La oferta debe tener entre 1 y 5 servicios")
        @Size(min = 1, max = 5, message = "La oferta debe tener entre 1 y 5 servicios")
        List<@NotNull(message = "El servicio es obligatorio") @Valid ServiceRequest> servicios
) {
}
