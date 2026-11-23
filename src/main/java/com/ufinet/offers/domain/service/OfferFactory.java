package com.ufinet.offers.domain.service;

import com.ufinet.offers.domain.exception.DomainValidationException;
import com.ufinet.offers.domain.exception.ValidationError;
import com.ufinet.offers.domain.model.ContractTerm;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferedService;
import com.ufinet.offers.domain.model.ServiceSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Crea ofertas en BORRADOR aplicando las invariantes del modelo de datos
 * y calculando el MRC de cada servicio con {@link PricingPolicy}.
 * Reúne todos los errores encontrados para devolverlos en una sola respuesta.
 */
public class OfferFactory {

    static final String CLIENT_FIELD = "cliente";
    static final String SERVICES_FIELD = "servicios";

    private final PricingPolicy pricingPolicy;

    public OfferFactory(PricingPolicy pricingPolicy) {
        this.pricingPolicy = Objects.requireNonNull(pricingPolicy, "pricingPolicy");
    }

    public Offer createDraft(String client, List<ServiceSpec> specs) {
        List<ValidationError> errors = new ArrayList<>();
        validateClient(client, errors);
        validateServices(specs, errors);
        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }
        List<OfferedService> services = specs.stream().map(this::toOfferedService).toList();
        return Offer.draft(client.trim(), services);
    }

    private OfferedService toOfferedService(ServiceSpec spec) {
        ContractTerm term = ContractTerm.fromMonths(spec.termMonths()).orElseThrow();
        return new OfferedService(null, spec.type(), spec.mbps(), term, spec.viable(),
                pricingPolicy.calculateMrc(spec.mbps(), term));
    }

    private void validateClient(String client, List<ValidationError> errors) {
        if (client == null || client.isBlank()) {
            errors.add(new ValidationError(CLIENT_FIELD, "El cliente es obligatorio"));
        } else if (client.trim().length() > Offer.MAX_CLIENT_LENGTH) {
            errors.add(new ValidationError(CLIENT_FIELD,
                    "El cliente no puede superar %d caracteres".formatted(Offer.MAX_CLIENT_LENGTH)));
        }
    }

    private void validateServices(List<ServiceSpec> specs, List<ValidationError> errors) {
        if (specs == null || specs.size() < Offer.MIN_SERVICES || specs.size() > Offer.MAX_SERVICES) {
            errors.add(new ValidationError(SERVICES_FIELD, "La oferta debe tener entre %d y %d servicios"
                    .formatted(Offer.MIN_SERVICES, Offer.MAX_SERVICES)));
            return;
        }
        for (int index = 0; index < specs.size(); index++) {
            validateService(specs.get(index), "%s[%d]".formatted(SERVICES_FIELD, index), errors);
        }
    }

    private void validateService(ServiceSpec spec, String path, List<ValidationError> errors) {
        if (spec == null) {
            errors.add(new ValidationError(path, "El servicio es obligatorio"));
            return;
        }
        if (spec.type() == null) {
            errors.add(new ValidationError(path + ".tipo", "El tipo es obligatorio (INTERNET o CAPACIDAD)"));
        }
        if (spec.mbps() == null) {
            errors.add(new ValidationError(path + ".mbps", "Los Mbps son obligatorios"));
        } else if (spec.mbps() < OfferedService.MIN_MBPS || spec.mbps() > OfferedService.MAX_MBPS) {
            errors.add(new ValidationError(path + ".mbps", "Los Mbps deben estar entre %d y %d"
                    .formatted(OfferedService.MIN_MBPS, OfferedService.MAX_MBPS)));
        }
        if (spec.termMonths() == null) {
            errors.add(new ValidationError(path + ".plazoMeses", "El plazo es obligatorio"));
        } else if (!ContractTerm.isAllowed(spec.termMonths())) {
            errors.add(new ValidationError(path + ".plazoMeses", "El plazo debe ser 12, 24 o 36 meses"));
        }
        if (spec.viable() == null) {
            errors.add(new ValidationError(path + ".viable", "El indicador viable es obligatorio"));
        }
    }
}
