package com.ufinet.offers.infrastructure.adapter.out.persistence.mapper;

import com.ufinet.offers.domain.model.ContractTerm;
import com.ufinet.offers.domain.model.Offer;
import com.ufinet.offers.domain.model.OfferedService;
import com.ufinet.offers.infrastructure.adapter.out.persistence.entity.OfferEntity;
import com.ufinet.offers.infrastructure.adapter.out.persistence.entity.ServiceEntity;

import java.util.List;

/**
 * Traduce entre el modelo de dominio y las entidades JPA.
 */
public final class OfferPersistenceMapper {

    private OfferPersistenceMapper() {
    }

    public static OfferEntity toEntity(Offer offer) {
        OfferEntity entity = new OfferEntity(offer.getId(), offer.getClient(), offer.getStatus(), offer.getVersion());
        offer.getServices().forEach(service -> entity.addService(toEntity(service)));
        return entity;
    }

    public static Offer toDomain(OfferEntity entity) {
        List<OfferedService> services = entity.getServices().stream()
                .map(OfferPersistenceMapper::toDomain)
                .toList();
        return new Offer(entity.getId(), entity.getClient(), entity.getStatus(), entity.getVersion(), services);
    }

    private static ServiceEntity toEntity(OfferedService service) {
        return new ServiceEntity(service.getId(), service.getType(), service.getMbps(), service.getTermMonths(),
                service.isViable(), service.getMrc());
    }

    private static OfferedService toDomain(ServiceEntity entity) {
        ContractTerm term = ContractTerm.fromMonths(entity.getTermMonths())
                .orElseThrow(() -> new IllegalStateException(
                        "Plazo persistido no soportado: " + entity.getTermMonths()));
        return new OfferedService(entity.getId(), entity.getType(), entity.getMbps(), term,
                Boolean.TRUE.equals(entity.getViable()), entity.getMrc());
    }
}
