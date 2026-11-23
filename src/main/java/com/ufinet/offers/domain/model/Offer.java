package com.ufinet.offers.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raíz: una oferta comercial con entre 1 y 5 servicios.
 * Es inmutable; los cambios de estado producen una nueva instancia.
 */
public final class Offer {

    public static final int MAX_CLIENT_LENGTH = 100;
    public static final int MIN_SERVICES = 1;
    public static final int MAX_SERVICES = 5;

    private final Long id;
    private final String client;
    private final OfferStatus status;
    private final Long version;
    private final List<OfferedService> services;

    public Offer(Long id, String client, OfferStatus status, Long version, List<OfferedService> services) {
        this.id = id;
        this.client = Objects.requireNonNull(client, "client");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
        this.services = List.copyOf(Objects.requireNonNull(services, "services"));
    }

    /** Crea una oferta nueva, siempre en estado BORRADOR. */
    public static Offer draft(String client, List<OfferedService> services) {
        return new Offer(null, client, OfferStatus.BORRADOR, null, services);
    }

    public Offer withStatus(OfferStatus newStatus) {
        return new Offer(id, client, newStatus, version, services);
    }

    public boolean isDraft() {
        return status == OfferStatus.BORRADOR;
    }

    public boolean allServicesViable() {
        return services.stream().allMatch(OfferedService::isViable);
    }

    /** Suma del MRC de todos los servicios, con 2 decimales. */
    public BigDecimal getMrcTotal() {
        return services.stream()
                .map(OfferedService::getMrc)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public String getClient() {
        return client;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public List<OfferedService> getServices() {
        return services;
    }
}
