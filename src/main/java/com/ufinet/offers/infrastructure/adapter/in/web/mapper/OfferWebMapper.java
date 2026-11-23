package com.ufinet.offers.infrastructure.adapter.in.web.mapper;

import com.ufinet.offers.application.port.in.CreateOfferCommand;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferedService;
import com.ufinet.offers.domain.model.ServiceSpec;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.CreateOfferRequest;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.OfferSummaryResponse;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.ServiceRequest;
import com.ufinet.offers.infrastructure.adapter.in.web.dto.ServiceResponse;

import java.util.List;
import java.util.Objects;

/**
 * Traduce entre los DTOs HTTP y el modelo de dominio.
 */
public final class OfferWebMapper {

    private OfferWebMapper() {
    }

    public static CreateOfferCommand toCommand(CreateOfferRequest request) {
        List<ServiceSpec> specs = request.servicios() == null
                ? List.of()
                : request.servicios().stream().filter(Objects::nonNull).map(OfferWebMapper::toSpec).toList();
        return new CreateOfferCommand(request.cliente(), specs);
    }

    public static OfferResponse toResponse(Offer offer) {
        List<ServiceResponse> services = offer.getServices().stream().map(OfferWebMapper::toResponse).toList();
        return new OfferResponse(offer.getId(), offer.getClient(), offer.getStatus(), offer.getVersion(),
                services, offer.getMrcTotal());
    }

    public static OfferSummaryResponse toSummary(Offer offer) {
        return new OfferSummaryResponse(offer.getId(), offer.getClient(), offer.getStatus(), offer.getMrcTotal());
    }

    private static ServiceSpec toSpec(ServiceRequest request) {
        return new ServiceSpec(request.tipo(), request.mbps(), request.plazoMeses(), request.viable());
    }

    private static ServiceResponse toResponse(OfferedService service) {
        return new ServiceResponse(service.getId(), service.getType(), service.getMbps(),
                service.getTermMonths(), service.isViable(), service.getMrc());
    }
}
